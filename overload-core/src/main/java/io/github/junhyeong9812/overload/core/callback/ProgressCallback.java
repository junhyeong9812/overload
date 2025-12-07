package io.github.junhyeong9812.overload.core.callback;

import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

/**
 * 부하 테스트 진행 상황을 콜백으로 전달하는 함수형 인터페이스.
 *
 * <p>테스트 실행 중 각 요청 완료 시 호출되어 진행 상황과 개별 요청 결과를 알린다.
 * CLI, GUI, 로깅, 웹소켓 브로드캐스트 등 다양한 방식으로 진행률을 표시할 수 있다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * // 기본 콜백 (진행률과 결과 모두 처리)
 * ProgressCallback callback = (completed, total, result) -> {
 *     System.out.printf("Progress: %d/%d%n", completed, total);
 *     switch (result) {
 *         case RequestResult.Success s -> System.out.println("  Status: " + s.statusCode());
 *         case RequestResult.Failure f -> System.out.println("  Error: " + f.errorMessage());
 *     }
 * };
 *
 * // 진행률만 처리하는 간단한 콜백
 * ProgressCallback callback = ProgressCallback.simple((completed, total) ->
 *     System.out.printf("\rProgress: %d/%d", completed, total)
 * );
 *
 * TestResult result = LoadTester.run(config, callback);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@FunctionalInterface
public interface ProgressCallback {

  /**
   * 진행 상황과 개별 요청 결과를 전달받는 콜백 메서드.
   *
   * <p>각 HTTP 요청이 완료될 때마다 호출된다.
   * 요청 성공/실패 여부와 관계없이 모든 완료된 요청에 대해 호출된다.
   *
   * @param completed 현재까지 완료된 요청 수 (1부터 시작)
   * @param total     전체 요청 수
   * @param result    개별 요청 결과 ({@link RequestResult.Success} 또는 {@link RequestResult.Failure})
   */
  void onProgress(int completed, int total, RequestResult result);

  /**
   * 진행률을 백분율로 계산한다.
   *
   * <p>편의 메서드로, 콜백 구현 시 진행률 계산에 사용할 수 있다.
   *
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @return 진행률 (0.0 ~ 100.0), total이 0이면 0.0 반환
   */
  default double getPercentage(int completed, int total) {
    return total > 0 ? (double) completed / total * 100 : 0;
  }

  /**
   * 아무 동작도 하지 않는 No-op 콜백을 반환한다.
   *
   * <p>진행 상황 모니터링이 필요 없는 경우에 사용한다.
   *
   * @return No-op 콜백 인스턴스
   */
  static ProgressCallback noop() {
    return (completed, total, result) -> {};
  }

  /**
   * 진행률만 처리하는 간단한 콜백을 생성한다.
   *
   * <p>개별 요청 결과는 무시하고 진행률만 필요한 경우에 사용한다.
   * 이전 버전과의 호환성을 위해 제공된다.
   *
   * <p><b>사용 예시:</b>
   * <pre>{@code
   * ProgressCallback callback = ProgressCallback.simple((completed, total) ->
   *     System.out.printf("Progress: %d/%d%n", completed, total)
   * );
   * }</pre>
   *
   * @param simpleCallback 진행률만 받는 간단한 콜백
   * @return {@link ProgressCallback} 인스턴스
   */
  static ProgressCallback simple(SimpleProgressCallback simpleCallback) {
    return (completed, total, result) -> simpleCallback.onProgress(completed, total);
  }

  /**
   * 진행률만 처리하는 간단한 콜백 인터페이스.
   *
   * <p>개별 요청 결과가 필요 없는 경우에 사용한다.
   * {@link #simple(SimpleProgressCallback)} 메서드와 함께 사용한다.
   *
   * @see #simple(SimpleProgressCallback)
   */
  @FunctionalInterface
  interface SimpleProgressCallback {

    /**
     * 진행 상황을 전달받는 콜백 메서드.
     *
     * @param completed 완료된 요청 수
     * @param total     전체 요청 수
     */
    void onProgress(int completed, int total);
  }
}