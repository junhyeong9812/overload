package io.github.junhyeong9812.overload.starter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Load test request DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record TestRequest(
    @NotBlank(message = "URL is required")
    String url,

    String method,

    Map<String, String> headers,

    String body,

    @Positive(message = "Concurrency must be positive")
    int concurrency,

    @Positive(message = "Total requests must be positive")
    int totalRequests,

    int timeoutMs
) {
  public TestRequest {
    if (method == null || method.isBlank()) {
      method = "GET";
    }
  }
}