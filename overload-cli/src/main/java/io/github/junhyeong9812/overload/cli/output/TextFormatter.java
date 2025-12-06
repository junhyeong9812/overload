package io.github.junhyeong9812.overload.cli.output;

import io.github.junhyeong9812.overload.core.metric.domain.Percentiles;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult.LatencyStats;

/**
 * 텍스트 형식 출력 포매터.
 *
 * <p>사람이 읽기 쉬운 형태로 결과를 출력한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class TextFormatter implements OutputFormatter {

  @Override
  public String format(TestResult result) {
    StringBuilder sb = new StringBuilder();

    sb.append("Results:\n");
    sb.append("=".repeat(50)).append("\n\n");

    // 요청 통계
    sb.append("  Requests\n");
    sb.append(String.format("    Total:       %,d%n", result.totalRequests()));
    sb.append(String.format("    Successful:  %,d (%.1f%%)%n",
        result.successCount(), result.successRate()));
    sb.append(String.format("    Failed:      %,d (%.1f%%)%n",
        result.failCount(), result.failRate()));
    sb.append("\n");

    // 성능 통계
    sb.append("  Performance\n");
    sb.append(String.format("    Total Time:  %.2fs%n",
        result.totalDuration().toMillis() / 1000.0));
    sb.append(String.format("    RPS:         %.2f req/s%n",
        result.requestsPerSecond()));
    sb.append("\n");

    // 지연 시간 통계
    LatencyStats stats = result.latencyStats();
    sb.append("  Latency\n");
    sb.append(String.format("    Min:         %,dms%n", stats.min()));
    sb.append(String.format("    Max:         %,dms%n", stats.max()));
    sb.append(String.format("    Avg:         %.2fms%n", stats.avg()));
    sb.append("\n");

    // 백분위수
    Percentiles p = stats.percentiles();
    sb.append("  Percentiles\n");
    sb.append(String.format("    p50:         %,dms%n", p.p50()));
    sb.append(String.format("    p90:         %,dms%n", p.p90()));
    sb.append(String.format("    p95:         %,dms%n", p.p95()));
    sb.append(String.format("    p99:         %,dms%n", p.p99()));

    return sb.toString();
  }
}