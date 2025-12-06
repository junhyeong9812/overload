package io.github.junhyeong9812.overload.core.metric.domain;

import java.time.Duration;

/**
 * 부하 테스트 결과를 표현하는 불변(Immutable) 레코드.
 *
 * <p>테스트 실행 후 최종 결과를 담으며, 요청 통계, 지연 시간 통계,
 * 처리량(RPS) 등을 포함한다.
 *
 * <p><b>포함 정보:</b>
 * <ul>
 *   <li>총 요청 수, 성공/실패 수</li>
 *   <li>전체 테스트 소요 시간</li>
 *   <li>초당 요청 수 (RPS)</li>
 *   <li>지연 시간 통계 (최소, 최대, 평균, 백분위수)</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * TestResult result = LoadTester.run(config);
 *
 * System.out.println("Total: " + result.totalRequests());
 * System.out.println("Success Rate: " + result.successRate() + "%");
 * System.out.println("RPS: " + result.requestsPerSecond());
 * System.out.println("P99: " + result.latencyStats().percentiles().p99() + "ms");
 * }</pre>
 *
 * @param totalRequests     총 요청 수
 * @param successCount      성공한 요청 수
 * @param failCount         실패한 요청 수
 * @param totalDuration     전체 테스트 소요 시간
 * @param requestsPerSecond 초당 요청 수 (RPS)
 * @param latencyStats      지연 시간 통계
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record TestResult(
    int totalRequests,
    int successCount,
    int failCount,
    Duration totalDuration,
    double requestsPerSecond,
    LatencyStats latencyStats
) {

  /**
   * 성공률을 계산한다.
   *
   * @return 성공률 (0.0 ~ 100.0), 요청이 없으면 0.0
   */
  public double successRate() {
    return totalRequests > 0
        ? (double) successCount / totalRequests * 100
        : 0;
  }

  /**
   * 실패율을 계산한다.
   *
   * @return 실패율 (0.0 ~ 100.0)
   */
  public double failRate() {
    return 100 - successRate();
  }

  /**
   * 지연 시간 통계를 표현하는 불변(Immutable) 레코드.
   *
   * @param min         최소 지연 시간 (밀리초)
   * @param max         최대 지연 시간 (밀리초)
   * @param avg         평균 지연 시간 (밀리초)
   * @param percentiles 백분위수 통계
   */
  public record LatencyStats(
      long min,
      long max,
      double avg,
      Percentiles percentiles
  ) {

    /**
     * 빈 LatencyStats를 생성한다.
     *
     * @return 모든 값이 0인 LatencyStats
     */
    public static LatencyStats empty() {
      return new LatencyStats(0, 0, 0, Percentiles.empty());
    }
  }
}