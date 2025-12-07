package io.github.junhyeong9812.overload.scenario.scenario.application.port;

import io.github.junhyeong9812.overload.scenario.scenario.application.callback.StepCallback;
import io.github.junhyeong9812.overload.scenario.scenario.domain.Scenario;
import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioResult;

/**
 * 시나리오 실행 포트 인터페이스.
 *
 * <p>시나리오를 1회 실행하고 결과를 반환한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public interface ScenarioExecutorPort {

  /**
   * 시나리오를 1회 실행한다.
   *
   * @param scenario 실행할 시나리오
   * @return 시나리오 실행 결과
   */
  ScenarioResult execute(Scenario scenario);

  /**
   * 시나리오를 1회 실행한다 (콜백 포함).
   *
   * @param scenario 실행할 시나리오
   * @param callback Step 완료 콜백
   * @return 시나리오 실행 결과
   */
  ScenarioResult execute(Scenario scenario, StepCallback callback);
}