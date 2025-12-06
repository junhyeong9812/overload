package io.github.junhyeong9812.overload.starter.dto;

import io.github.junhyeong9812.overload.core.metric.domain.Percentiles;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

/**
 * Test result response DTO for JSON serialization.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record TestResultResponse(
    int totalRequests,
    int successCount,
    int failCount,
    long totalDurationMs,
    double requestsPerSecond,
    double successRate,
    LatencyStatsResponse latencyStats
) {
  public static TestResultResponse from(TestResult result) {
    return new TestResultResponse(
        result.totalRequests(),
        result.successCount(),
        result.failCount(),
        result.totalDuration().toMillis(),
        result.requestsPerSecond(),
        result.successRate(),
        LatencyStatsResponse.from(result.latencyStats())
    );
  }

  public record LatencyStatsResponse(
      long min,
      long max,
      double avg,
      PercentilesResponse percentiles
  ) {
    public static LatencyStatsResponse from(TestResult.LatencyStats stats) {
      return new LatencyStatsResponse(
          stats.min(),
          stats.max(),
          stats.avg(),
          PercentilesResponse.from(stats.percentiles())
      );
    }
  }

  public record PercentilesResponse(
      long p50,
      long p90,
      long p95,
      long p99
  ) {
    public static PercentilesResponse from(Percentiles p) {
      return new PercentilesResponse(p.p50(), p.p90(), p.p95(), p.p99());
    }
  }
}