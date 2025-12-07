package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.Map;

/**
 * Step 실행 결과를 담는 불변(Immutable) 레코드.
 *
 * <p>단일 Step의 실행 결과를 표현하며, 성공 시 추출된 값도 포함한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * if (result.success()) {
 *     Object token = result.extractedValues().get("token");
 * } else {
 *     System.err.println("Failed: " + result.error());
 * }
 * }</pre>
 *
 * @param stepId          Step ID
 * @param stepName        Step 이름
 * @param success         성공 여부
 * @param statusCode      HTTP 상태 코드 (실패 시 -1)
 * @param latencyMs       지연 시간 (밀리초)
 * @param extractedValues 추출된 값 맵
 * @param error           에러 메시지 (성공 시 null)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record StepResult(
    String stepId,
    String stepName,
    boolean success,
    int statusCode,
    long latencyMs,
    Map<String, Object> extractedValues,
    String error
) {

  /**
   * StepResult의 Compact Constructor.
   */
  public StepResult {
    if (extractedValues == null) {
      extractedValues = Map.of();
    } else {
      extractedValues = Map.copyOf(extractedValues);
    }
  }

  /**
   * 성공 결과를 생성한다.
   *
   * @param stepId          Step ID
   * @param stepName        Step 이름
   * @param statusCode      HTTP 상태 코드
   * @param latencyMs       지연 시간
   * @param extractedValues 추출된 값
   * @return 성공 StepResult
   */
  public static StepResult success(
      String stepId,
      String stepName,
      int statusCode,
      long latencyMs,
      Map<String, Object> extractedValues
  ) {
    return new StepResult(stepId, stepName, true, statusCode, latencyMs, extractedValues, null);
  }

  /**
   * 실패 결과를 생성한다.
   *
   * @param stepId    Step ID
   * @param stepName  Step 이름
   * @param latencyMs 지연 시간
   * @param error     에러 메시지
   * @return 실패 StepResult
   */
  public static StepResult failure(String stepId, String stepName, long latencyMs, String error) {
    return new StepResult(stepId, stepName, false, -1, latencyMs, Map.of(), error);
  }

  /**
   * HTTP 상태 코드와 함께 실패 결과를 생성한다.
   *
   * @param stepId     Step ID
   * @param stepName   Step 이름
   * @param statusCode HTTP 상태 코드
   * @param latencyMs  지연 시간
   * @param error      에러 메시지
   * @return 실패 StepResult
   */
  public static StepResult failure(
      String stepId,
      String stepName,
      int statusCode,
      long latencyMs,
      String error
  ) {
    return new StepResult(stepId, stepName, false, statusCode, latencyMs, Map.of(), error);
  }
}