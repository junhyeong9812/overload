package io.github.junhyeong9812.overload.starter.service;

import io.github.junhyeong9812.overload.core.LoadTester;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.starter.OverloadProperties;
import io.github.junhyeong9812.overload.starter.dto.ProgressMessage;
import io.github.junhyeong9812.overload.starter.dto.RequestLog;
import io.github.junhyeong9812.overload.starter.dto.TestRequest;
import io.github.junhyeong9812.overload.starter.dto.TestResultResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 부하 테스트 실행 서비스.
 *
 * <p>부하 테스트의 시작, 중지, 상태 조회, 실시간 진행 상황 브로드캐스트 등을 관리한다.
 * 다중 테스트를 병렬로 실행할 수 있으며, 각 테스트의 개별 요청 로그를 실시간으로 수집한다.
 *
 * <p><b>주요 기능:</b>
 * <ul>
 *   <li>부하 테스트 시작 및 중지</li>
 *   <li>실시간 진행 상황 WebSocket 브로드캐스트</li>
 *   <li>개별 요청 로그 수집 및 조회</li>
 *   <li>테스트 이력 관리</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class LoadTestService {

  /** 보관할 최근 요청 로그의 최대 개수 */
  private static final int MAX_RECENT_LOGS = 50;

  private final OverloadProperties properties;
  private final ResultBroadcastService broadcastService;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  private final Map<String, TestExecution> runningTests = new ConcurrentHashMap<>();
  private final List<TestHistory> testHistory = Collections.synchronizedList(new ArrayList<>());

  /**
   * LoadTestService를 생성한다.
   *
   * @param properties       Overload 설정 프로퍼티
   * @param broadcastService WebSocket 브로드캐스트 서비스
   */
  public LoadTestService(
      OverloadProperties properties,
      ResultBroadcastService broadcastService) {
    this.properties = properties;
    this.broadcastService = broadcastService;
  }

  /**
   * 새로운 부하 테스트를 시작한다.
   *
   * <p>테스트는 비동기로 실행되며, 진행 상황은 WebSocket을 통해 실시간으로 브로드캐스트된다.
   *
   * @param testId  테스트 고유 ID
   * @param request 테스트 요청 정보
   */
  public void startTest(String testId, TestRequest request) {
    LoadTestConfig config = buildConfig(request);

    TestExecution execution = new TestExecution(testId, request, config.totalRequests());
    runningTests.put(testId, execution);

    executor.submit(() -> executeTest(testId, config, execution, request));
  }

  /**
   * 부하 테스트를 실행한다.
   *
   * <p>테스트 완료 또는 실패 시 결과를 저장하고 브로드캐스트한다.
   *
   * @param testId    테스트 ID
   * @param config    부하 테스트 설정
   * @param execution 테스트 실행 정보
   * @param request   테스트 요청 정보
   */
  private void executeTest(String testId, LoadTestConfig config, TestExecution execution, TestRequest request) {
    try {
      TestResult result = LoadTester.run(config, (completed, total, requestResult) -> {
        if (execution.isCancelled()) {
          return;
        }
        execution.setCompleted(completed);

        RequestLog log = convertToLog(completed, requestResult);
        execution.addLog(log);

        int broadcastInterval = Math.max(1, Math.min(total / 100, 10));
        if (completed % broadcastInterval == 0 || completed == total) {
          broadcastProgress(testId, request, execution, "RUNNING");
        }
      });

      if (!execution.isCancelled()) {
        execution.complete(result);
        saveHistory(testId, request, result);
        broadcastProgress(testId, request, execution, "COMPLETED");
      }

    } catch (Exception e) {
      execution.fail(e.getMessage());
      broadcastProgress(testId, request, execution, "FAILED");
    } finally {
      scheduleCleanup(testId);
    }
  }

  /**
   * RequestResult를 RequestLog로 변환한다.
   *
   * @param requestNumber 요청 순번
   * @param requestResult 요청 결과
   * @return 변환된 RequestLog
   */
  private RequestLog convertToLog(int requestNumber, RequestResult requestResult) {
    return switch (requestResult) {
      case RequestResult.Success s -> RequestLog.success(
          requestNumber,
          s.statusCode(),
          s.latencyMs()
      );
      case RequestResult.Failure f -> RequestLog.failure(
          requestNumber,
          f.latencyMs(),
          f.errorMessage()
      );
    };
  }

  /**
   * 진행 상황을 WebSocket으로 브로드캐스트한다.
   *
   * @param testId    테스트 ID
   * @param request   테스트 요청 정보
   * @param execution 테스트 실행 정보
   * @param status    현재 상태
   */
  private void broadcastProgress(String testId, TestRequest request, TestExecution execution, String status) {
    List<RequestLog> recentLogs = execution.getRecentLogs(10);
    broadcastService.broadcast(ProgressMessage.withLogs(
        testId,
        request.url(),
        request.method(),
        execution.getCompleted(),
        execution.getTotal(),
        status,
        recentLogs
    ));
  }

  /**
   * 테스트 상태를 조회한다.
   *
   * @param testId 테스트 ID
   * @return 테스트 상태 정보 Map
   */
  public Map<String, Object> getTestStatus(String testId) {
    TestExecution execution = runningTests.get(testId);
    if (execution == null) {
      return Map.of("status", "NOT_FOUND", "testId", testId);
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("testId", testId);
    response.put("status", execution.getStatus());
    response.put("completed", execution.getCompleted());
    response.put("total", execution.getTotal());
    response.put("percentage", execution.getPercentage());
    response.put("recentLogs", execution.getRecentLogs(20));

    if (execution.getResult() != null) {
      response.put("result", TestResultResponse.from(execution.getResult()));
    }
    if (execution.getError() != null) {
      response.put("error", execution.getError());
    }

    return response;
  }

  /**
   * 실행 중인 테스트를 중지한다.
   *
   * @param testId 테스트 ID
   */
  public void stopTest(String testId) {
    TestExecution execution = runningTests.get(testId);
    if (execution != null) {
      execution.cancel();
      broadcastService.broadcast(ProgressMessage.of(
          testId,
          execution.getRequest().url(),
          execution.getRequest().method(),
          execution.getCompleted(),
          execution.getTotal(),
          "CANCELLED"
      ));
    }
  }

  /**
   * 모든 활성 테스트 목록을 조회한다.
   *
   * @return 활성 테스트 정보 목록
   */
  public List<Map<String, Object>> getAllTests() {
    return runningTests.values().stream()
        .map(exec -> {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("testId", exec.getTestId());
          map.put("url", exec.getRequest().url());
          map.put("method", exec.getRequest().method());
          map.put("status", exec.getStatus());
          map.put("completed", exec.getCompleted());
          map.put("total", exec.getTotal());
          map.put("percentage", exec.getPercentage());
          return map;
        })
        .toList();
  }

  /**
   * 최근 완료된 테스트 이력을 조회한다.
   *
   * @param limit 조회할 최대 개수
   * @return 테스트 이력 목록
   */
  public List<Map<String, Object>> getRecentTests(int limit) {
    return testHistory.stream()
        .limit(limit)
        .map(h -> {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("testId", h.testId());
          map.put("url", h.request().url());
          map.put("method", h.request().method());
          map.put("totalRequests", h.result().totalRequests());
          map.put("successRate", h.result().successRate());
          map.put("rps", h.result().requestsPerSecond());
          map.put("timestamp", h.timestamp().toString());
          return map;
        })
        .toList();
  }

  /**
   * 테스트 결과를 이력에 저장한다.
   *
   * @param testId  테스트 ID
   * @param request 테스트 요청 정보
   * @param result  테스트 결과
   */
  private void saveHistory(String testId, TestRequest request, TestResult result) {
    testHistory.add(0, new TestHistory(testId, request, result, Instant.now()));
    if (testHistory.size() > 100) {
      testHistory.remove(testHistory.size() - 1);
    }
  }

  /**
   * 테스트 정리 작업을 예약한다.
   *
   * <p>5분 후 runningTests에서 해당 테스트를 제거한다.
   *
   * @param testId 테스트 ID
   */
  private void scheduleCleanup(String testId) {
    executor.submit(() -> {
      try {
        Thread.sleep(300000); // 5 minutes
        runningTests.remove(testId);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
  }

  /**
   * TestRequest를 LoadTestConfig로 변환한다.
   *
   * @param request 테스트 요청 정보
   * @return 부하 테스트 설정
   */
  private LoadTestConfig buildConfig(TestRequest request) {
    var defaults = properties.getDefaults();

    return LoadTestConfig.builder()
        .url(request.url())
        .method(HttpMethod.valueOf(request.method().toUpperCase()))
        .headers(request.headers() != null ? request.headers() : Map.of())
        .body(request.body())
        .concurrency(request.concurrency() > 0 ? request.concurrency() : defaults.getConcurrency())
        .totalRequests(request.totalRequests() > 0 ? request.totalRequests() : defaults.getRequests())
        .timeout(request.timeoutMs() > 0
            ? Duration.ofMillis(request.timeoutMs())
            : defaults.getTimeout())
        .build();
  }

  // ========================================
  // Inner Classes
  // ========================================

  /**
   * 테스트 실행 상태를 관리하는 내부 클래스.
   */
  private static class TestExecution {
    private final String testId;
    private final TestRequest request;
    private final int total;
    private final Deque<RequestLog> recentLogs = new ConcurrentLinkedDeque<>();
    private volatile String status = "RUNNING";
    private volatile int completed = 0;
    private volatile TestResult result;
    private volatile String error;
    private volatile boolean cancelled = false;

    /**
     * TestExecution을 생성한다.
     *
     * @param testId  테스트 ID
     * @param request 테스트 요청 정보
     * @param total   전체 요청 수
     */
    public TestExecution(String testId, TestRequest request, int total) {
      this.testId = testId;
      this.request = request;
      this.total = total;
    }

    /**
     * 요청 로그를 추가한다.
     *
     * @param log 추가할 로그
     */
    public void addLog(RequestLog log) {
      recentLogs.addFirst(log);
      while (recentLogs.size() > MAX_RECENT_LOGS) {
        recentLogs.removeLast();
      }
    }

    /**
     * 최근 요청 로그를 조회한다.
     *
     * @param count 조회할 개수
     * @return 로그 목록
     */
    public List<RequestLog> getRecentLogs(int count) {
      return recentLogs.stream().limit(count).toList();
    }

    /**
     * 완료된 요청 수를 설정한다.
     *
     * @param completed 완료된 요청 수
     */
    public void setCompleted(int completed) {
      this.completed = completed;
    }

    /**
     * 테스트를 완료 상태로 설정한다.
     *
     * @param result 테스트 결과
     */
    public void complete(TestResult result) {
      this.result = result;
      this.status = "COMPLETED";
    }

    /**
     * 테스트를 실패 상태로 설정한다.
     *
     * @param error 에러 메시지
     */
    public void fail(String error) {
      this.error = error;
      this.status = "FAILED";
    }

    /**
     * 테스트를 취소한다.
     */
    public void cancel() {
      this.cancelled = true;
      this.status = "CANCELLED";
    }

    /**
     * 테스트가 취소되었는지 확인한다.
     *
     * @return 취소 여부
     */
    public boolean isCancelled() {
      return cancelled;
    }

    public String getTestId() {
      return testId;
    }

    public TestRequest getRequest() {
      return request;
    }

    public String getStatus() {
      return status;
    }

    public int getCompleted() {
      return completed;
    }

    public int getTotal() {
      return total;
    }

    /**
     * 진행률을 계산한다.
     *
     * @return 진행률 (0.0 ~ 100.0)
     */
    public double getPercentage() {
      return total > 0 ? (double) completed / total * 100 : 0;
    }

    public TestResult getResult() {
      return result;
    }

    public String getError() {
      return error;
    }
  }

  /**
   * 완료된 테스트 이력을 저장하는 레코드.
   */
  private record TestHistory(String testId, TestRequest request, TestResult result, Instant timestamp) {
  }
}