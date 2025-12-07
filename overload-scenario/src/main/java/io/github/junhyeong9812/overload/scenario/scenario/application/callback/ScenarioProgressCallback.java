package io.github.junhyeong9812.overload.scenario.scenario.application.callback;

import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioResult;

/**
 * 시나리오 반복 실행 중 진행 상황을 전달받는 콜백 인터페이스.
 *
 * <p>부하 테스트 진행 중 실시간 상태 업데이트에 사용한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@FunctionalInterface
public interface ScenarioProgressCallback {

  /**
   * 시나리오 1회 완료 시 호출된다.
   *
   * @param completed 완료된 시나리오 수
   * @param total     전체 시나리오 수
   * @param result    방금 완료된 시나리오 결과
   */
  void onProgress(int completed, int total, ScenarioResult result);

  /**
   * 아무 동작도 하지 않는 콜백을 반환한다.
   *
   * @return no-op 콜백
   */
  static ScenarioProgressCallback noop() {
    return (completed, total, result) -> {};
  }
}