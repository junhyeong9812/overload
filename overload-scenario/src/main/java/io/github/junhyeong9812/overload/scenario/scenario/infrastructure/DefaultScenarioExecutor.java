package io.github.junhyeong9812.overload.scenario.scenario.infrastructure;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.http.application.port.DetailedHttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.infrastructure.JdkDetailedHttpClient;
import io.github.junhyeong9812.overload.scenario.extract.application.port.ValueExtractorPort;
import io.github.junhyeong9812.overload.scenario.extract.infrastructure.HeaderExtractor;
import io.github.junhyeong9812.overload.scenario.extract.infrastructure.JsonPathExtractor;
import io.github.junhyeong9812.overload.scenario.extract.infrastructure.RegexExtractor;
import io.github.junhyeong9812.overload.scenario.scenario.application.callback.StepCallback;
import io.github.junhyeong9812.overload.scenario.scenario.application.port.ScenarioExecutorPort;
import io.github.junhyeong9812.overload.scenario.scenario.domain.*;
import io.github.junhyeong9812.overload.scenario.variable.application.VariableResolver;
import io.github.junhyeong9812.overload.scenario.variable.domain.VariableContext;

import java.time.Duration;
import java.util.*;

/**
 * 시나리오 실행기 기본 구현체.
 *
 * <p>시나리오의 각 Step을 순차적으로 실행하고,
 * 응답에서 값을 추출하여 다음 Step에 전달한다.
 *
 * <p><b>실행 흐름:</b>
 * <ol>
 *   <li>Step의 URL, Header, Body에서 변수 치환</li>
 *   <li>HTTP 요청 전송</li>
 *   <li>응답에서 값 추출 및 컨텍스트에 저장</li>
 *   <li>실패 전략에 따른 처리</li>
 *   <li>다음 Step으로 진행</li>
 * </ol>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class DefaultScenarioExecutor implements ScenarioExecutorPort {

  private final DetailedHttpClientPort httpClient;
  private final List<ValueExtractorPort> extractors;
  private final VariableResolver variableResolver;

  /**
   * 기본 설정으로 DefaultScenarioExecutor를 생성한다.
   *
   * @param timeout HTTP 요청 타임아웃
   */
  public DefaultScenarioExecutor(Duration timeout) {
    this.httpClient = new JdkDetailedHttpClient(timeout);
    this.extractors = List.of(
        new JsonPathExtractor(),
        new HeaderExtractor(),
        new RegexExtractor()
    );
    this.variableResolver = new VariableResolver();
  }

  /**
   * 커스텀 의존성으로 DefaultScenarioExecutor를 생성한다.
   *
   * @param httpClient       HTTP 클라이언트
   * @param extractors       값 추출기 목록
   * @param variableResolver 변수 치환기
   */
  public DefaultScenarioExecutor(
      DetailedHttpClientPort httpClient,
      List<ValueExtractorPort> extractors,
      VariableResolver variableResolver
  ) {
    this.httpClient = httpClient;
    this.extractors = extractors;
    this.variableResolver = variableResolver;
  }

  @Override
  public ScenarioResult execute(Scenario scenario) {
    return execute(scenario, StepCallback.noop());
  }

  @Override
  public ScenarioResult execute(Scenario scenario, StepCallback callback) {
    long startTime = System.nanoTime();
    VariableContext context = new VariableContext();
    List<StepResult> stepResults = new ArrayList<>();

    for (ScenarioStep step : scenario.steps()) {
      StepResult stepResult = executeStep(step, context, scenario);
      stepResults.add(stepResult);
      callback.onStepComplete(step.id(), stepResult);

      if (!stepResult.success()) {
        // 실패 전략 처리
        if (scenario.failureStrategy() == FailureStrategy.STOP) {
          return ScenarioResult.failure(
              scenario.name(),
              toMillis(startTime),
              stepResults,
              scenario.stepCount(),
              step.id(),
              stepResult.error()
          );
        } else if (scenario.failureStrategy() == FailureStrategy.RETRY) {
          // 재시도 로직
          StepResult retryResult = retryStep(step, context, scenario);
          if (retryResult != null && retryResult.success()) {
            // 마지막 실패 결과를 성공 결과로 교체
            stepResults.set(stepResults.size() - 1, retryResult);
          } else {
            return ScenarioResult.failure(
                scenario.name(),
                toMillis(startTime),
                stepResults,
                scenario.stepCount(),
                step.id(),
                "Retry exhausted: " + stepResult.error()
            );
          }
        }
        // SKIP 전략은 그냥 계속 진행
      }
    }

    return ScenarioResult.success(
        scenario.name(),
        toMillis(startTime),
        stepResults
    );
  }

  /**
   * 단일 Step을 실행한다.
   */
  private StepResult executeStep(ScenarioStep step, VariableContext context, Scenario scenario) {
    long stepStartTime = System.nanoTime();

    try {
      // 변수 치환
      String resolvedUrl = variableResolver.resolve(step.url(), context);
      Map<String, String> resolvedHeaders = variableResolver.resolveHeaders(step.headers(), context);
      String resolvedBody = step.body() != null
          ? variableResolver.resolve(step.body(), context)
          : null;

      // HTTP 요청 생성
      HttpRequest request = HttpRequest.from(
          resolvedUrl,
          step.method(),
          resolvedHeaders,
          resolvedBody
      );

      // 요청 전송
      DetailedRequestResult result = httpClient.send(request);

      if (result instanceof DetailedRequestResult.Success success) {
        // HTTP 상태 코드 확인
        if (!success.isHttpSuccess()) {
          return StepResult.failure(
              step.id(),
              step.name(),
              success.statusCode(),
              success.latencyMs(),
              "HTTP " + success.statusCode()
          );
        }

        // 값 추출
        Map<String, Object> extractedValues = extractValues(success, step.extract());

        // 컨텍스트에 저장
        extractedValues.forEach((varName, value) -> context.put(step.id(), varName, value));

        return StepResult.success(
            step.id(),
            step.name(),
            success.statusCode(),
            success.latencyMs(),
            extractedValues
        );

      } else if (result instanceof DetailedRequestResult.Failure failure) {
        return StepResult.failure(
            step.id(),
            step.name(),
            failure.latencyMs(),
            failure.errorType() + ": " + failure.errorMessage()
        );
      }

      // Unreachable
      return StepResult.failure(step.id(), step.name(), toMillis(stepStartTime), "Unknown result type");

    } catch (Exception e) {
      return StepResult.failure(
          step.id(),
          step.name(),
          toMillis(stepStartTime),
          e.getClass().getSimpleName() + ": " + e.getMessage()
      );
    }
  }

  /**
   * Step을 재시도한다.
   */
  private StepResult retryStep(ScenarioStep step, VariableContext context, Scenario scenario) {
    for (int i = 0; i < scenario.retryCount(); i++) {
      try {
        Thread.sleep(scenario.retryDelayMs());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
      }

      StepResult result = executeStep(step, context, scenario);
      if (result.success()) {
        return result;
      }
    }
    return null;
  }

  /**
   * 응답에서 값을 추출한다.
   */
  private Map<String, Object> extractValues(
      DetailedRequestResult.Success response,
      Map<String, String> extractPaths
  ) {
    Map<String, Object> extracted = new HashMap<>();

    extractPaths.forEach((varName, path) -> {
      for (ValueExtractorPort extractor : extractors) {
        if (extractor.supports(path)) {
          extractor.extract(response, path)
              .ifPresent(value -> extracted.put(varName, value));
          break;
        }
      }
    });

    return extracted;
  }

  /**
   * 나노초를 밀리초로 변환한다.
   */
  private long toMillis(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }
}