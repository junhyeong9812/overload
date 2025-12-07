package io.github.junhyeong9812.overload.scenario.scenario.infrastructure.callback;

import io.github.junhyeong9812.overload.scenario.scenario.application.callback.StepCallback;
import io.github.junhyeong9812.overload.scenario.scenario.domain.StepResult;

import java.io.PrintStream;

/**
 * 콘솔에 Step 진행 상황을 출력하는 콜백 구현체.
 *
 * <p>CLI나 디버깅 용도로 사용한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ScenarioResult result = executor.execute(scenario, new ConsoleStepCallback());
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ConsoleStepCallback implements StepCallback {

  private final PrintStream out;

  /**
   * 기본 출력 스트림(System.out)으로 생성한다.
   */
  public ConsoleStepCallback() {
    this.out = System.out;
  }

  /**
   * 지정된 출력 스트림으로 생성한다.
   *
   * @param out 출력 스트림
   */
  public ConsoleStepCallback(PrintStream out) {
    this.out = out;
  }

  @Override
  public void onStepComplete(String stepId, StepResult result) {
    String status = result.success() ? "✓" : "✗";
    String statusCode = result.statusCode() > 0 ? String.valueOf(result.statusCode()) : "-";

    out.printf("  %s [%s] %s - %dms (HTTP %s)%n",
        status,
        stepId,
        result.stepName(),
        result.latencyMs(),
        statusCode
    );

    if (!result.success() && result.error() != null) {
      out.printf("    └─ Error: %s%n", result.error());
    }

    if (!result.extractedValues().isEmpty()) {
      result.extractedValues().forEach((key, value) ->
          out.printf("    └─ Extracted %s = %s%n", key, value)
      );
    }
  }
}