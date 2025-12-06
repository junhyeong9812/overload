package io.github.junhyeong9812.overload.core.metric.domain;

/**
 * 지연 시간 백분위수를 표현하는 불변(Immutable) 레코드.
 *
 * <p>부하 테스트 결과의 지연 시간 분포를 백분위수로 나타낸다.
 * P50(중앙값), P90, P95, P99 및 최소/최대값을 포함한다.
 *
 * <p><b>백분위수 의미:</b>
 * <ul>
 *   <li>P50 - 전체 요청의 50%가 이 시간 이하로 완료</li>
 *   <li>P90 - 전체 요청의 90%가 이 시간 이하로 완료</li>
 *   <li>P95 - 전체 요청의 95%가 이 시간 이하로 완료</li>
 *   <li>P99 - 전체 요청의 99%가 이 시간 이하로 완료</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * Percentiles percentiles = new Percentiles(50, 100, 150, 200, 10, 500);
 * System.out.println("P99: " + percentiles.p99() + "ms");
 * }</pre>
 *
 * @param p50 50번째 백분위수 (밀리초)
 * @param p90 90번째 백분위수 (밀리초)
 * @param p95 95번째 백분위수 (밀리초)
 * @param p99 99번째 백분위수 (밀리초)
 * @param min 최소 지연 시간 (밀리초)
 * @param max 최대 지연 시간 (밀리초)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record Percentiles(
    long p50,
    long p90,
    long p95,
    long p99,
    long min,
    long max
) {

  /**
   * 빈 Percentiles를 생성한다.
   *
   * @return 모든 값이 0인 Percentiles
   */
  public static Percentiles empty() {
    return new Percentiles(0, 0, 0, 0, 0, 0);
  }
}