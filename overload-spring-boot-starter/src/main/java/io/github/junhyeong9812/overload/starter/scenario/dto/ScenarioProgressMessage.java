package io.github.junhyeong9812.overload.starter.scenario.dto;

/**
 * WebSocket으로 전송되는 시나리오 진행 상황 메시지.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioProgressMessage(
    String testId,
    int completed,
    int total,
    double progress,
    int successCount,
    int failCount,
    double successRate,
    String lastStepId,
    boolean lastStepSuccess
) {
  /**
   * 진행 상황 메시지를 생성한다.
   *
   * @param testId          테스트 ID
   * @param completed       완료된 시나리오 수
   * @param total           전체 시나리오 수
   * @param successCount    성공 수
   * @param failCount       실패 수
   * @param lastStepId      마지막 완료된 Step ID
   * @param lastStepSuccess 마지막 Step 성공 여부
   * @return 진행 상황 메시지
   */
  public static ScenarioProgressMessage of(
      String testId,
      int completed,
      int total,
      int successCount,
      int failCount,
      String lastStepId,
      boolean lastStepSuccess
  ) {
    double progress = total > 0 ? (double) completed / total * 100 : 0;
    double successRate = completed > 0 ? (double) successCount / completed * 100 : 0;

    return new ScenarioProgressMessage(
        testId,
        completed,
        total,
        progress,
        successCount,
        failCount,
        successRate,
        lastStepId,
        lastStepSuccess
    );
  }
}