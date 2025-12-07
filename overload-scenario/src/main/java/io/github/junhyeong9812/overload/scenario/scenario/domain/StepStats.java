package io.github.junhyeong9812.overload.scenario.scenario.domain;

/**
 * Step별 통계 정보를 담는 불변(Immutable) 레코드.
 *
 * <p>여러 번의 시나리오 실행에서 특정 Step의 성능 통계를 집계한다.
 *
 * @param stepId       Step ID
 * @param stepName     Step 이름
 * @param totalCount   총 실행 횟수
 * @param successCount 성공 횟수
 * @param failCount    실패 횟수
 * @param minLatencyMs 최소 지연 시간 (밀리초)
 * @param maxLatencyMs 최대 지연 시간 (밀리초)
 * @param avgLatencyMs 평균 지연 시간 (밀리초)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record StepStats(
    String stepId,
    String stepName,
    int totalCount,
    int successCount,
    int failCount,
    long minLatencyMs,
    long maxLatencyMs,
    double avgLatencyMs
) {

  /**
   * 성공률을 계산한다.
   *
   * @return 성공률 (0.0 ~ 100.0)
   */
  public double successRate() {
    return totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
  }
}