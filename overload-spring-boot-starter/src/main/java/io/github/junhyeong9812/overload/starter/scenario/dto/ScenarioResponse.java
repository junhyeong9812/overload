package io.github.junhyeong9812.overload.starter.scenario.dto;

import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioTestResult;

import java.util.List;

/**
 * 시나리오 테스트 결과 응답 DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioResponse(
    String testId,
    String status,
    String scenarioName,
    int totalIterations,
    int successCount,
    int failCount,
    long totalDurationMs,
    double avgDurationMs,
    double successRate,
    double scenariosPerSecond,
    List<ScenarioStepResponse> stepStats
) {
  /**
   * ScenarioTestResult에서 응답 DTO를 생성한다.
   *
   * @param testId 테스트 ID
   * @param status 테스트 상태
   * @param result 테스트 결과
   * @return 응답 DTO
   */
  public static ScenarioResponse from(String testId, String status, ScenarioTestResult result) {
    List<ScenarioStepResponse> steps = result.stepStats().values().stream()
        .map(ScenarioStepResponse::from)
        .toList();

    return new ScenarioResponse(
        testId,
        status,
        result.scenarioName(),
        result.totalIterations(),
        result.successCount(),
        result.failCount(),
        result.totalDurationMs(),
        result.avgDurationMs(),
        result.successRate(),
        result.scenariosPerSecond(),
        steps
    );
  }

  /**
   * 진행 중 상태의 응답을 생성한다.
   *
   * @param testId       테스트 ID
   * @param scenarioName 시나리오 이름
   * @param completed    완료된 수
   * @param total        전체 수
   * @return 진행 중 응답 DTO
   */
  public static ScenarioResponse running(String testId, String scenarioName, int completed, int total) {
    return new ScenarioResponse(
        testId,
        "RUNNING",
        scenarioName,
        total,
        0,
        0,
        0,
        0,
        0,
        0,
        List.of()
    );
  }
}