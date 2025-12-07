package io.github.junhyeong9812.overload.scenario.builder;

import io.github.junhyeong9812.overload.scenario.scenario.domain.FailureStrategy;
import io.github.junhyeong9812.overload.scenario.scenario.domain.Scenario;
import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Scenario를 생성하는 빌더.
 *
 * <p>Fluent API로 시나리오를 정의할 수 있다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * Scenario scenario = Scenario.builder()
 *     .name("주문 플로우")
 *     .failureStrategy(FailureStrategy.STOP)
 *     .step("login", step -> step
 *         .post("http://auth/api/login")
 *         .body("{\"username\":\"test\"}")
 *         .extract("token", "$.data.accessToken"))
 *     .step("order", step -> step
 *         .post("http://order/api/orders")
 *         .header("Authorization", "Bearer ${login.token}"))
 *     .build();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ScenarioBuilder {

  private String name;
  private final List<ScenarioStep> steps = new ArrayList<>();
  private FailureStrategy failureStrategy = FailureStrategy.STOP;
  private int retryCount = 0;
  private long retryDelayMs = 1000;

  /**
   * 시나리오 이름을 설정한다.
   *
   * @param name 시나리오 이름
   * @return this
   */
  public ScenarioBuilder name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Step을 추가한다.
   *
   * @param stepId     Step 고유 식별자
   * @param configurer Step 설정 Consumer
   * @return this
   */
  public ScenarioBuilder step(String stepId, Consumer<StepBuilder> configurer) {
    StepBuilder builder = new StepBuilder(stepId);
    configurer.accept(builder);
    steps.add(builder.build());
    return this;
  }

  /**
   * 이미 생성된 Step을 추가한다.
   *
   * @param step 추가할 Step
   * @return this
   */
  public ScenarioBuilder step(ScenarioStep step) {
    steps.add(step);
    return this;
  }

  /**
   * 실패 처리 전략을 설정한다.
   *
   * @param strategy 실패 처리 전략
   * @return this
   */
  public ScenarioBuilder failureStrategy(FailureStrategy strategy) {
    this.failureStrategy = strategy;
    return this;
  }

  /**
   * 재시도 횟수를 설정한다.
   *
   * <p>RETRY 전략에서만 사용된다.
   *
   * @param count 재시도 횟수
   * @return this
   */
  public ScenarioBuilder retryCount(int count) {
    this.retryCount = count;
    return this;
  }

  /**
   * 재시도 간격을 설정한다.
   *
   * <p>RETRY 전략에서만 사용된다.
   *
   * @param delayMs 재시도 간격 (밀리초)
   * @return this
   */
  public ScenarioBuilder retryDelayMs(long delayMs) {
    this.retryDelayMs = delayMs;
    return this;
  }

  /**
   * Scenario를 생성한다.
   *
   * @return 생성된 Scenario
   * @throws IllegalArgumentException 이름이 없거나 Step이 없는 경우
   */
  public Scenario build() {
    return new Scenario(
        name,
        steps,
        failureStrategy,
        retryCount,
        retryDelayMs
    );
  }
}