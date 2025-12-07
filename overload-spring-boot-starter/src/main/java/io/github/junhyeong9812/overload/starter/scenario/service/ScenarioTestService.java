package io.github.junhyeong9812.overload.starter.scenario.service;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.scenario.scenario.application.callback.ScenarioProgressCallback;
import io.github.junhyeong9812.overload.scenario.scenario.application.service.ScenarioLoadTester;
import io.github.junhyeong9812.overload.scenario.scenario.domain.*;
import io.github.junhyeong9812.overload.starter.scenario.dto.*;
import io.github.junhyeong9812.overload.starter.service.ResultBroadcastService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 시나리오 테스트 실행 서비스.
 *
 * <p>시나리오 테스트를 비동기로 실행하고 WebSocket으로 진행 상황을 전송한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@Service
public class ScenarioTestService {

  private final ResultBroadcastService broadcastService;
  private final Map<String, ScenarioTestResult> completedTests = new ConcurrentHashMap<>();
  private final Map<String, String> runningTests = new ConcurrentHashMap<>();

  /**
   * ScenarioTestService를 생성한다.
   *
   * @param broadcastService 브로드캐스트 서비스
   */
  public ScenarioTestService(ResultBroadcastService broadcastService) {
    this.broadcastService = broadcastService;
  }

  /**
   * 시나리오 테스트를 시작한다.
   *
   * @param request 테스트 요청
   * @return 테스트 ID
   */
  public String startTest(ScenarioRequest request) {
    String testId = UUID.randomUUID().toString().substring(0, 8);

    Scenario scenario = convertToScenario(request);

    runningTests.put(testId, scenario.name());

    Thread.startVirtualThread(() -> {
      try {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ScenarioProgressCallback callback = (completed, total, result) -> {
          if (result.success()) {
            successCount.incrementAndGet();
          } else {
            failCount.incrementAndGet();
          }

          String lastStepId = result.stepResults().isEmpty()
              ? ""
              : result.stepResults().get(result.stepResults().size() - 1).stepId();

          ScenarioProgressMessage message = ScenarioProgressMessage.of(
              testId,
              completed,
              total,
              successCount.get(),
              failCount.get(),
              lastStepId,
              result.success()
          );

          broadcastService.broadcastScenarioProgress(testId, message);
        };

        ScenarioTestResult result = ScenarioLoadTester.run(
            scenario,
            request.iterations(),
            request.concurrency(),
            Duration.ofMillis(request.timeoutMs()),
            callback
        );

        completedTests.put(testId, result);
        runningTests.remove(testId);

        ScenarioResponse response = ScenarioResponse.from(testId, "COMPLETED", result);
        broadcastService.broadcastScenarioComplete(testId, response);

      } catch (Exception e) {
        runningTests.remove(testId);
        broadcastService.broadcastScenarioError(testId, e.getMessage() != null ? e.getMessage() : "Unknown error");
      }
    });

    return testId;
  }

  /**
   * 테스트 결과를 조회한다.
   *
   * @param testId 테스트 ID
   * @return 테스트 응답 (없으면 null)
   */
  public ScenarioResponse getResult(String testId) {
    if (runningTests.containsKey(testId)) {
      return ScenarioResponse.running(testId, runningTests.get(testId), 0, 0);
    }

    ScenarioTestResult result = completedTests.get(testId);
    if (result != null) {
      return ScenarioResponse.from(testId, "COMPLETED", result);
    }

    return null;
  }

  /**
   * 진행 중인 테스트 목록을 반환한다.
   *
   * @return 테스트 ID → 시나리오 이름 맵
   */
  public Map<String, String> getRunningTests() {
    return Map.copyOf(runningTests);
  }

  private Scenario convertToScenario(ScenarioRequest request) {
    var steps = request.steps().stream()
        .map(this::convertToStep)
        .toList();

    FailureStrategy strategy;
    try {
      strategy = FailureStrategy.valueOf(request.failureStrategy().toUpperCase());
    } catch (IllegalArgumentException e) {
      strategy = FailureStrategy.STOP;
    }

    return new Scenario(
        request.name(),
        steps,
        strategy,
        request.retryCount(),
        request.retryDelayMs()
    );
  }

  private ScenarioStep convertToStep(ScenarioStepRequest request) {
    HttpMethod method;
    try {
      method = HttpMethod.valueOf(request.method().toUpperCase());
    } catch (IllegalArgumentException e) {
      method = HttpMethod.GET;
    }

    return new ScenarioStep(
        request.id(),
        request.name(),
        method,
        request.url(),
        request.headers(),
        request.body(),
        request.extract(),
        request.timeoutMs()
    );
  }
}