package io.github.junhyeong9812.overload.starter.scenario.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 시나리오 Step 요청 DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioStepRequest(
    @NotBlank(message = "Step ID is required")
    String id,

    String name,

    String method,

    @NotBlank(message = "URL is required")
    String url,

    Map<String, String> headers,

    String body,

    Map<String, String> extract,

    Long timeoutMs
) {
  /**
   * 기본값을 적용한 Compact Constructor.
   */
  public ScenarioStepRequest {
    if (method == null || method.isBlank()) {
      method = "GET";
    }
    if (name == null || name.isBlank()) {
      name = id;
    }
    if (headers == null) {
      headers = Map.of();
    }
    if (extract == null) {
      extract = Map.of();
    }
    if (timeoutMs == null || timeoutMs <= 0) {
      timeoutMs = 30000L;
    }
  }
}