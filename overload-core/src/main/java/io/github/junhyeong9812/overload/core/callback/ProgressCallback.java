package io.github.junhyeong9812.overload.core.callback;

/**
 * 부하 테스트 진행 상황을 콜백으로 전달하는 함수형 인터페이스.
 *
 * <p>테스트 실행 중 각 요청 완료 시 호출되어 진행 상황을 알린다.
 * CLI, GUI, 로깅 등 다양한 방식으로 진행률을 표시할 수 있다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * // 콘솔 출력 콜백
 * ProgressCallback callback = (completed, total) ->
 *     System.out.printf("\rProgress: %d/%d (%.1f%%)%n",
 *         completed, total, (double) completed / total * 100);
 *
 * TestResult result = LoadTester.run(config, callback);
 *
 * // 무시 콜백 (기본)
 * TestResult result = LoadTester.run(config, ProgressCallback.noop());
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@FunctionalInterface
public interface ProgressCallback {

  /**
   * 진행 상황을 전달받는 콜백 메서드.
   *
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   */
  void onProgress(int completed, int total);

  /**
   * 진행률을 백분율로 계산한다.
   *
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @return 진행률 (0.0 ~ 100.0)
   */
  default double getPercentage(int completed, int total) {
    return total > 0 ? (double) completed / total * 100 : 0;
  }

  /**
   * 아무 동작도 하지 않는 콜백을 반환한다.
   *
   * @return No-op 콜백 인스턴스
   */
  static ProgressCallback noop() {
    return (completed, total) -> {};
  }
}