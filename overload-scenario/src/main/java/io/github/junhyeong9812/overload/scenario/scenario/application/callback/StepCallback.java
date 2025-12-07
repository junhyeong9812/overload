package io.github.junhyeong9812.overload.scenario.scenario.application.callback;

import io.github.junhyeong9812.overload.scenario.scenario.domain.StepResult;

/**
 * Step 완료 시 호출되는 콜백 인터페이스.
 *
 * <p>각 Step 실행 완료 후 진행 상황을 전달받을 때 사용한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@FunctionalInterface
public interface StepCallback {

  /**
   * Step 완료 시 호출된다.
   *
   * @param stepId Step ID
   * @param result Step 실행 결과
   */
  void onStepComplete(String stepId, StepResult result);

  /**
   * 아무 동작도 하지 않는 콜백을 반환한다.
   *
   * @return no-op 콜백
   */
  static StepCallback noop() {
    return (stepId, result) -> {};
  }
}