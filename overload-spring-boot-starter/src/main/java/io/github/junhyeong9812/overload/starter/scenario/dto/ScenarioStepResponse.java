package io.github.junhyeong9812.overload.starter.scenario.dto;

import io.github.junhyeong9812.overload.scenario.scenario.domain.StepStats;

/**
 * Step 통계 응답 DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioStepResponse(
    String stepId,
    String stepName,
    int totalCount,
    int successCount,
    int failCount,
    double successRate,
    long minLatencyMs,
    long maxLatencyMs,
    double avgLatencyMs
) {
  /**
   * StepStats에서 응답 DTO를 생성한다.
   *
   * @param stats Step 통계
   * @return 응답 DTO
   */
  public static ScenarioStepResponse from(StepStats stats) {
    return new ScenarioStepResponse(
        stats.stepId(),
        stats.stepName(),
        stats.totalCount(),
        stats.successCount(),
        stats.failCount(),
        stats.successRate(),
        stats.minLatencyMs(),
        stats.maxLatencyMs(),
        stats.avgLatencyMs()
    );
  }
}