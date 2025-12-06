package io.github.junhyeong9812.overload.starter.service;

import io.github.junhyeong9812.overload.core.LoadTester;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.starter.OverloadProperties;
import io.github.junhyeong9812.overload.starter.dto.ProgressMessage;
import io.github.junhyeong9812.overload.starter.dto.TestRequest;
import io.github.junhyeong9812.overload.starter.dto.TestResultResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Load test execution service.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class LoadTestService {

  private final OverloadProperties properties;
  private final ResultBroadcastService broadcastService;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  private final Map<String, TestExecution> runningTests = new ConcurrentHashMap<>();
  private final List<TestHistory> testHistory = Collections.synchronizedList(new ArrayList<>());

  public LoadTestService(
      OverloadProperties properties,
      ResultBroadcastService broadcastService) {
    this.properties = properties;
    this.broadcastService = broadcastService;
  }

  public void startTest(String testId, TestRequest request) {
    LoadTestConfig config = buildConfig(request);

    TestExecution execution = new TestExecution(testId, request, config.totalRequests());
    runningTests.put(testId, execution);

    executor.submit(() -> executeTest(testId, config, execution, request));
  }

  private void executeTest(String testId, LoadTestConfig config, TestExecution execution, TestRequest request) {
    try {
      TestResult result = LoadTester.run(config, (completed, total) -> {
        if (execution.isCancelled()) {
          return;
        }
        execution.setCompleted(completed);

        // Throttle broadcasts (every 1%)
        if (completed % Math.max(1, total / 100) == 0 || completed == total) {
          broadcastService.broadcast(new ProgressMessage(
              testId, completed, total, "RUNNING"
          ));
        }
      });

      if (!execution.isCancelled()) {
        execution.complete(result);
        saveHistory(testId, request, result);

        broadcastService.broadcast(new ProgressMessage(
            testId, config.totalRequests(), config.totalRequests(), "COMPLETED"
        ));
      }

    } catch (Exception e) {
      execution.fail(e.getMessage());
      broadcastService.broadcast(new ProgressMessage(
          testId, execution.getCompleted(), execution.getTotal(), "FAILED"
      ));
    } finally {
      // Keep in map for result retrieval, remove after timeout
      executor.submit(() -> {
        try {
          Thread.sleep(60000); // 1 minute
          runningTests.remove(testId);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    }
  }

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

    if (execution.getResult() != null) {
      response.put("result", TestResultResponse.from(execution.getResult()));
    }
    if (execution.getError() != null) {
      response.put("error", execution.getError());
    }

    return response;
  }

  public void stopTest(String testId) {
    TestExecution execution = runningTests.get(testId);
    if (execution != null) {
      execution.cancel();
      broadcastService.broadcast(new ProgressMessage(
          testId, execution.getCompleted(), execution.getTotal(), "CANCELLED"
      ));
    }
  }

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

  private void saveHistory(String testId, TestRequest request, TestResult result) {
    testHistory.add(0, new TestHistory(testId, request, result, Instant.now()));
    if (testHistory.size() > 100) {
      testHistory.remove(testHistory.size() - 1);
    }
  }

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

  // Inner classes

  private static class TestExecution {
    private final String testId;
    private final TestRequest request;
    private final int total;
    private volatile String status = "RUNNING";
    private volatile int completed = 0;
    private volatile TestResult result;
    private volatile String error;
    private volatile boolean cancelled = false;

    public TestExecution(String testId, TestRequest request, int total) {
      this.testId = testId;
      this.request = request;
      this.total = total;
    }

    public void setCompleted(int completed) {
      this.completed = completed;
    }

    public void complete(TestResult result) {
      this.result = result;
      this.status = "COMPLETED";
    }

    public void fail(String error) {
      this.error = error;
      this.status = "FAILED";
    }

    public void cancel() {
      this.cancelled = true;
      this.status = "CANCELLED";
    }

    public boolean isCancelled() {
      return cancelled;
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

  private record TestHistory(String testId, TestRequest request, TestResult result, Instant timestamp) {
  }
}