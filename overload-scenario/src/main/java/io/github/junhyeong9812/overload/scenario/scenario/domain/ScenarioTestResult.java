package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.Map;

/**
 * 시나리오 부하 테스트 전체 결과를 담는 불변(Immutable) 레코드.
 *
 * <p>여러 번의 시나리오 반복 실행 결과를 집계한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ScenarioTestResult result = ScenarioLoadTester.run(scenario, 100, 10);
 *
 * System.out.println("Success Rate: " + result.successRate() + "%");
 * System.out.println("Scenarios/sec: " + result.scenariosPerSecond());
 *
 * result.stepStats().forEach((stepId, stats) -> {
 *     System.out.println(stepId + ": avg=" + stats.avgLatencyMs() + "ms");
 * });
 * }</pre>
 *
 * @param scenarioName       시나리오 이름
 * @param totalIterations    총 반복 횟수
 * @param successCount       성공한 시나리오 수
 * @param failCount          실패한 시나리오 수
 * @param totalDurationMs    전체 테스트 소요 시간 (밀리초)
 * @param avgDurationMs      평균 시나리오 소요 시간 (밀리초)
 * @param successRate        성공률 (0.0 ~ 100.0)
 * @param scenariosPerSecond 초당 시나리오 처리 수
 * @param stepStats          Step별 통계 (stepId → StepStats)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioTestResult(
    String scenarioName,
    int totalIterations,
    int successCount,
    int failCount,
    long totalDurationMs,
    double avgDurationMs,
    double successRate,
    double scenariosPerSecond,
    Map<String, StepStats> stepStats
) {

  /**
   * ScenarioTestResult의 Compact Constructor.
   */
  public ScenarioTestResult {
    if (stepStats == null) {
      stepStats = Map.of();
    } else {
      stepStats = Map.copyOf(stepStats);
    }
  }

  /**
   * 결과 요약 문자열을 생성한다.
   *
   * @return 포맷팅된 요약 문자열
   */
  public String summary() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("=".repeat(60)).append("\n");
    sb.append("Scenario Test Result: ").append(scenarioName).append("\n");
    sb.append("=".repeat(60)).append("\n");
    sb.append(String.format("  Total Iterations:    %,d%n", totalIterations));
    sb.append(String.format("  Success:             %,d (%.1f%%)%n", successCount, successRate));
    sb.append(String.format("  Failed:              %,d%n", failCount));
    sb.append(String.format("  Total Duration:      %,dms%n", totalDurationMs));
    sb.append(String.format("  Avg Duration:        %.2fms%n", avgDurationMs));
    sb.append(String.format("  Scenarios/sec:       %.2f%n", scenariosPerSecond));
    sb.append("\n");
    sb.append("Step Statistics:\n");
    sb.append("-".repeat(60)).append("\n");
    sb.append(String.format("  %-15s %8s %10s %10s%n", "Step", "Success%", "Avg(ms)", "Max(ms)"));
    sb.append("-".repeat(60)).append("\n");

    stepStats.values().forEach(stats -> {
      sb.append(String.format("  %-15s %7.1f%% %10.0f %10d%n",
          stats.stepId(),
          stats.successRate(),
          stats.avgLatencyMs(),
          stats.maxLatencyMs()));
    });

    sb.append("=".repeat(60)).append("\n");
    return sb.toString();
  }
}