package io.github.junhyeong9812.overload.scenario.scenario.application.service;

import io.github.junhyeong9812.overload.scenario.scenario.application.callback.ScenarioProgressCallback;
import io.github.junhyeong9812.overload.scenario.scenario.application.port.ScenarioExecutorPort;
import io.github.junhyeong9812.overload.scenario.scenario.domain.*;
import io.github.junhyeong9812.overload.scenario.scenario.infrastructure.DefaultScenarioExecutor;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 시나리오 부하 테스트 실행기.
 *
 * <p>시나리오를 반복 실행하며 Virtual Thread로 병렬 처리한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * Scenario scenario = Scenario.builder()
 *     .name("Login Flow")
 *     .step("login", step -> step.post("/api/login"))
 *     .build();
 *
 * ScenarioTestResult result = ScenarioLoadTester.run(
 *     scenario,
 *     100,   // 100회 반복
 *     10,    // 동시 10개
 *     Duration.ofSeconds(30)
 * );
 *
 * System.out.println(result.summary());
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ScenarioLoadTester {

  private ScenarioLoadTester() {
    // Static utility class
  }

  /**
   * 시나리오 부하 테스트를 실행한다.
   *
   * @param scenario    실행할 시나리오
   * @param iterations  반복 횟수
   * @param concurrency 동시 실행 수
   * @param timeout     HTTP 요청 타임아웃
   * @return 테스트 결과
   */
  public static ScenarioTestResult run(
      Scenario scenario,
      int iterations,
      int concurrency,
      Duration timeout
  ) {
    return run(scenario, iterations, concurrency, timeout, ScenarioProgressCallback.noop());
  }

  /**
   * 시나리오 부하 테스트를 실행한다 (콜백 포함).
   *
   * @param scenario    실행할 시나리오
   * @param iterations  반복 횟수
   * @param concurrency 동시 실행 수
   * @param timeout     HTTP 요청 타임아웃
   * @param callback    진행 상황 콜백
   * @return 테스트 결과
   */
  public static ScenarioTestResult run(
      Scenario scenario,
      int iterations,
      int concurrency,
      Duration timeout,
      ScenarioProgressCallback callback
  ) {
    long startTime = System.nanoTime();

    // 통계 수집용
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    AtomicInteger completed = new AtomicInteger(0);
    LongAdder totalDuration = new LongAdder();

    // Step별 통계
    Map<String, StepStatsCollector> stepStatsMap = new ConcurrentHashMap<>();
    scenario.steps().forEach(step ->
        stepStatsMap.put(step.id(), new StepStatsCollector(step.id(), step.name()))
    );

    // 동시성 제한
    Semaphore semaphore = new Semaphore(concurrency);

    // Virtual Thread로 실행
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < iterations; i++) {
        executor.submit(() -> {
          try {
            semaphore.acquire();

            ScenarioExecutorPort scenarioExecutor = new DefaultScenarioExecutor(timeout);
            ScenarioResult result = scenarioExecutor.execute(scenario);

            // 통계 업데이트
            if (result.success()) {
              successCount.incrementAndGet();
            } else {
              failCount.incrementAndGet();
            }
            totalDuration.add(result.totalDurationMs());

            // Step 통계 업데이트
            result.stepResults().forEach(stepResult -> {
              StepStatsCollector collector = stepStatsMap.get(stepResult.stepId());
              if (collector != null) {
                collector.record(stepResult);
              }
            });

            // 콜백 호출
            int current = completed.incrementAndGet();
            callback.onProgress(current, iterations, result);

          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            semaphore.release();
          }
        });
      }
    }

    long totalTestDuration = (System.nanoTime() - startTime) / 1_000_000;

    // Step 통계 생성
    Map<String, StepStats> stepStats = new LinkedHashMap<>();
    scenario.steps().forEach(step -> {
      StepStatsCollector collector = stepStatsMap.get(step.id());
      if (collector != null) {
        stepStats.put(step.id(), collector.toStepStats());
      }
    });

    // 결과 생성
    int total = successCount.get() + failCount.get();
    double avgDuration = total > 0 ? (double) totalDuration.sum() / total : 0;
    double successRate = total > 0 ? (double) successCount.get() / total * 100 : 0;
    double scenariosPerSecond = totalTestDuration > 0
        ? (double) total / totalTestDuration * 1000
        : 0;

    return new ScenarioTestResult(
        scenario.name(),
        iterations,
        successCount.get(),
        failCount.get(),
        totalTestDuration,
        avgDuration,
        successRate,
        scenariosPerSecond,
        stepStats
    );
  }

  /**
   * Step별 통계 수집기.
   */
  private static class StepStatsCollector {
    private final String stepId;
    private final String stepName;
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final LongAdder totalLatency = new LongAdder();
    private volatile long minLatency = Long.MAX_VALUE;
    private volatile long maxLatency = Long.MIN_VALUE;

    StepStatsCollector(String stepId, String stepName) {
      this.stepId = stepId;
      this.stepName = stepName;
    }

    synchronized void record(StepResult result) {
      totalCount.incrementAndGet();
      totalLatency.add(result.latencyMs());

      if (result.success()) {
        successCount.incrementAndGet();
      } else {
        failCount.incrementAndGet();
      }

      if (result.latencyMs() < minLatency) {
        minLatency = result.latencyMs();
      }
      if (result.latencyMs() > maxLatency) {
        maxLatency = result.latencyMs();
      }
    }

    StepStats toStepStats() {
      int total = totalCount.get();
      double avg = total > 0 ? (double) totalLatency.sum() / total : 0;
      long min = minLatency == Long.MAX_VALUE ? 0 : minLatency;
      long max = maxLatency == Long.MIN_VALUE ? 0 : maxLatency;

      return new StepStats(
          stepId,
          stepName,
          total,
          successCount.get(),
          failCount.get(),
          min,
          max,
          avg
      );
    }
  }
}