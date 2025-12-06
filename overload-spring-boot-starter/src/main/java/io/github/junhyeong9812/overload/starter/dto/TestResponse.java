package io.github.junhyeong9812.overload.starter.dto;

/**
 * Load test start response DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record TestResponse(
    String testId,
    String status
) {
}