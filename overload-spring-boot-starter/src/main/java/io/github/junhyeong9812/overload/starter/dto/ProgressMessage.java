package io.github.junhyeong9812.overload.starter.dto;

/**
 * WebSocket progress message DTO.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ProgressMessage(
    String testId,
    int completed,
    int total,
    String status
) {
  public double percentage() {
    return total > 0 ? (double) completed / total * 100 : 0;
  }
}