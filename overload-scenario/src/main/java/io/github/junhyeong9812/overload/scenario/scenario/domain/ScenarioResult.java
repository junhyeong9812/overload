package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.List;

/**
 * 시나리오 1회 실행 결과를 담는 불변(Immutable) 레코드.
 *
 * <p>시나리오의 모든 Step 실행 결과와 전체 성공/실패 여부를 포함한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ScenarioResult result = executor.execute(scenario);
 *
 * if (result.success()) {
 *     System.out.println("Total time: " + result.totalDurationMs() + "ms");
 * } else {
 *     System.out.println("Failed at: " + result.failedAtStep());
 * }
 * }</pre>
 *
 * @param scenarioName    시나리오 이름
 * @param success         전체 성공 여부
 * @param totalDurationMs 전체 소요 시간 (밀리초)
 * @param stepResults     각 Step의 실행 결과
 * @param completedSteps  완료된 Step 수
 * @param totalSteps      전체 Step 수
 * @param failedAtStep    실패한 Step ID (성공 시 null)
 * @param failureReason   실패 사유 (성공 시 null)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioResult(
    String scenarioName,
    boolean success,
    long totalDurationMs,
    List<StepResult> stepResults,
    int completedSteps,
    int totalSteps,
    String failedAtStep,
    String failureReason
) {

  /**
   * ScenarioResult의 Compact Constructor.
   */
  public ScenarioResult {
    if (stepResults == null) {
      stepResults = List.of();
    } else {
      stepResults = List.copyOf(stepResults);
    }
  }

  /**
   * 성공 결과를 생성한다.
   *
   * @param scenarioName    시나리오 이름
   * @param totalDurationMs 전체 소요 시간
   * @param stepResults     Step 결과 목록
   * @return 성공 ScenarioResult
   */
  public static ScenarioResult success(
      String scenarioName,
      long totalDurationMs,
      List<StepResult> stepResults
  ) {
    return new ScenarioResult(
        scenarioName,
        true,
        totalDurationMs,
        stepResults,
        stepResults.size(),
        stepResults.size(),
        null,
        null
    );
  }

  /**
   * 실패 결과를 생성한다.
   *
   * @param scenarioName    시나리오 이름
   * @param totalDurationMs 전체 소요 시간
   * @param stepResults     실행된 Step 결과 목록
   * @param totalSteps      전체 Step 수
   * @param failedAtStep    실패한 Step ID
   * @param failureReason   실패 사유
   * @return 실패 ScenarioResult
   */
  public static ScenarioResult failure(
      String scenarioName,
      long totalDurationMs,
      List<StepResult> stepResults,
      int totalSteps,
      String failedAtStep,
      String failureReason
  ) {
    return new ScenarioResult(
        scenarioName,
        false,
        totalDurationMs,
        stepResults,
        stepResults.size(),
        totalSteps,
        failedAtStep,
        failureReason
    );
  }

  /**
   * 성공률을 계산한다.
   *
   * @return 성공률 (0.0 ~ 100.0)
   */
  public double successRate() {
    if (totalSteps == 0) return 0;
    long successCount = stepResults.stream().filter(StepResult::success).count();
    return (double) successCount / totalSteps * 100;
  }
}