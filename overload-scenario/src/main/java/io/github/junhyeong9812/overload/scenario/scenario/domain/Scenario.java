package io.github.junhyeong9812.overload.scenario.scenario.domain;

import io.github.junhyeong9812.overload.scenario.builder.ScenarioBuilder;

import java.util.List;

/**
 * 시나리오 정의를 담는 불변(Immutable) 레코드.
 *
 * <p>여러 Step으로 구성된 순차적 API 호출 시나리오를 정의한다.
 * 각 Step은 이전 Step의 응답에서 추출한 값을 사용할 수 있다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * Scenario scenario = Scenario.builder()
 *     .name("주문 플로우")
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
 * @param name            시나리오 이름
 * @param steps           Step 목록 (순차 실행)
 * @param failureStrategy 실패 처리 전략
 * @param retryCount      재시도 횟수 (RETRY 전략 시)
 * @param retryDelayMs    재시도 간격 (밀리초)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record Scenario(
    String name,
    List<ScenarioStep> steps,
    FailureStrategy failureStrategy,
    int retryCount,
    long retryDelayMs
) {

  /**
   * Scenario의 Compact Constructor.
   *
   * <p>불변 리스트로 변환하고 기본값을 설정한다.
   */
  public Scenario {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Scenario name must not be blank");
    }
    if (steps == null || steps.isEmpty()) {
      throw new IllegalArgumentException("Scenario must have at least one step");
    }
    steps = List.copyOf(steps);
    if (failureStrategy == null) {
      failureStrategy = FailureStrategy.STOP;
    }
    if (retryCount < 0) {
      retryCount = 0;
    }
    if (retryDelayMs < 0) {
      retryDelayMs = 0;
    }
  }

  /**
   * ScenarioBuilder를 생성한다.
   *
   * @return 새로운 ScenarioBuilder 인스턴스
   */
  public static ScenarioBuilder builder() {
    return new ScenarioBuilder();
  }

  /**
   * Step 개수를 반환한다.
   *
   * @return Step 개수
   */
  public int stepCount() {
    return steps.size();
  }

  /**
   * ID로 Step을 조회한다.
   *
   * @param stepId Step ID
   * @return 해당 Step, 없으면 null
   */
  public ScenarioStep getStep(String stepId) {
    return steps.stream()
        .filter(s -> s.id().equals(stepId))
        .findFirst()
        .orElse(null);
  }
}