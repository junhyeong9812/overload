package io.github.junhyeong9812.overload.scenario.scenario.domain;

/**
 * 시나리오 실패 처리 전략.
 *
 * <p>Step 실패 시 시나리오 전체의 동작을 결정한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public enum FailureStrategy {

  /**
   * 실패 시 즉시 시나리오 중단 (기본값).
   *
   * <p>하나의 Step이 실패하면 나머지 Step을 실행하지 않고
   * 시나리오 전체를 실패로 처리한다.
   */
  STOP,

  /**
   * 실패한 Step을 스킵하고 계속 진행.
   *
   * <p>실패한 Step은 건너뛰고 다음 Step을 실행한다.
   * 단, 실패한 Step에서 추출해야 할 변수가 있으면
   * 후속 Step에서 변수 참조 오류가 발생할 수 있다.
   */
  SKIP,

  /**
   * 지정된 횟수만큼 재시도 후 실패 시 중단.
   *
   * <p>Step이 실패하면 설정된 재시도 횟수만큼 다시 시도한다.
   * 모든 재시도가 실패하면 STOP과 동일하게 시나리오를 중단한다.
   */
  RETRY
}