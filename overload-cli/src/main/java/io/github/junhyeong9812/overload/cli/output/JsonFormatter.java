package io.github.junhyeong9812.overload.cli.output;

import io.github.junhyeong9812.overload.core.metric.domain.Percentiles;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult.LatencyStats;

/**
 * JSON 형식 출력 포매터.
 *
 * <p>프로그래밍 방식으로 처리하기 쉬운 JSON 형태로 결과를 출력한다.
 * 외부 라이브러리 없이 직접 JSON을 생성한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class JsonFormatter implements OutputFormatter {

  @Override
  public String format(TestResult result) {
    LatencyStats stats = result.latencyStats();
    Percentiles p = stats.percentiles();

    return """
                {
                  "requests": {
                    "total": %d,
                    "successful": %d,
                    "failed": %d,
                    "successRate": %.2f
                  },
                  "performance": {
                    "totalDurationMs": %d,
                    "requestsPerSecond": %.2f
                  },
                  "latency": {
                    "min": %d,
                    "max": %d,
                    "avg": %.2f,
                    "percentiles": {
                      "p50": %d,
                      "p90": %d,
                      "p95": %d,
                      "p99": %d
                    }
                  }
                }""".formatted(
        result.totalRequests(),
        result.successCount(),
        result.failCount(),
        result.successRate(),
        result.totalDuration().toMillis(),
        result.requestsPerSecond(),
        stats.min(),
        stats.max(),
        stats.avg(),
        p.p50(),
        p.p90(),
        p.p95(),
        p.p99()
    );
  }
}