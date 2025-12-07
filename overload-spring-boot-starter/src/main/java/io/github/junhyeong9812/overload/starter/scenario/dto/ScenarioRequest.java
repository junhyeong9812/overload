package io.github.junhyeong9812.overload.starter.scenario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 시나리오 테스트 요청 DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioRequest(
    @NotBlank(message = "Scenario name is required")
    String name,

    @NotEmpty(message = "At least one step is required")
    @Valid
    List<ScenarioStepRequest> steps,

    String failureStrategy,

    Integer retryCount,

    Long retryDelayMs,

    @Min(value = 1, message = "Iterations must be at least 1")
    @Max(value = 100000, message = "Iterations must be at most 100000")
    Integer iterations,

    @Min(value = 1, message = "Concurrency must be at least 1")
    @Max(value = 1000, message = "Concurrency must be at most 1000")
    Integer concurrency,

    @Min(value = 1000, message = "Timeout must be at least 1000ms")
    @Max(value = 300000, message = "Timeout must be at most 300000ms")
    Long timeoutMs
) {
  /**
   * 기본값을 적용한 Compact Constructor.
   */
  public ScenarioRequest {
    if (failureStrategy == null || failureStrategy.isBlank()) {
      failureStrategy = "STOP";
    }
    if (retryCount == null || retryCount < 0) {
      retryCount = 0;
    }
    if (retryDelayMs == null || retryDelayMs < 0) {
      retryDelayMs = 1000L;
    }
    if (iterations == null || iterations <= 0) {
      iterations = 1;
    }
    if (concurrency == null || concurrency <= 0) {
      concurrency = 1;
    }
    if (timeoutMs == null || timeoutMs <= 0) {
      timeoutMs = 30000L;
    }
  }
}