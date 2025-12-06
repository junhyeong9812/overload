package io.github.junhyeong9812.overload.core.metric.application;

import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.metric.domain.Percentiles;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult.LatencyStats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;

/**
 * 부하 테스트 메트릭을 수집하고 집계하는 클래스.
 *
 * <p>테스트 실행 중 각 요청의 결과를 기록하고,
 * 테스트 종료 후 최종 결과를 집계한다.
 * 동시성을 지원하며, 여러 스레드에서 안전하게 호출 가능하다.
 *
 * <p><b>사용 흐름:</b>
 * <ol>
 *   <li>{@link #start()} - 테스트 시작 시간 기록</li>
 *   <li>{@link #record(RequestResult)} - 각 요청 결과 기록</li>
 *   <li>{@link #end()} - 테스트 종료 시간 기록</li>
 *   <li>{@link #aggregate()} - 최종 결과 집계</li>
 * </ol>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * MetricAggregator aggregator = new MetricAggregator();
 *
 * aggregator.start();
 * for (RequestResult result : results) {
 *     aggregator.record(result);
 * }
 * aggregator.end();
 *
 * TestResult testResult = aggregator.aggregate();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see TestResult
 */
public class MetricAggregator {

  private final LongAdder totalRequests = new LongAdder();
  private final LongAdder successCount = new LongAdder();
  private final LongAdder failCount = new LongAdder();
  private final List<Long> latencies = new CopyOnWriteArrayList<>();

  private long startTime;
  private long endTime;

  /**
   * 테스트 시작 시간을 기록한다.
   */
  public void start() {
    this.startTime = System.currentTimeMillis();
  }

  /**
   * 테스트 종료 시간을 기록한다.
   */
  public void end() {
    this.endTime = System.currentTimeMillis();
  }

  /**
   * 요청 결과를 기록한다.
   *
   * <p>이 메서드는 스레드 안전하며, 여러 Virtual Thread에서 동시에 호출 가능하다.
   *
   * @param result 기록할 요청 결과
   */
  public void record(RequestResult result) {
    totalRequests.increment();
    latencies.add(result.latencyMs());

    if (result instanceof RequestResult.Success success) {
      if (success.isHttpSuccess()) {
        successCount.increment();
      } else {
        failCount.increment();
      }
    } else {
      failCount.increment();
    }
  }

  /**
   * 수집된 메트릭을 집계하여 최종 결과를 반환한다.
   *
   * @return 테스트 결과
   */
  public TestResult aggregate() {
    int total = totalRequests.intValue();
    int success = successCount.intValue();
    int fail = failCount.intValue();

    Duration duration = Duration.ofMillis(endTime - startTime);
    double rps = duration.toMillis() > 0
        ? (double) total / duration.toMillis() * 1000
        : 0;

    LatencyStats latencyStats = calculateLatencyStats();

    return new TestResult(total, success, fail, duration, rps, latencyStats);
  }

  /**
   * 지연 시간 통계를 계산한다.
   */
  private LatencyStats calculateLatencyStats() {
    if (latencies.isEmpty()) {
      return LatencyStats.empty();
    }

    List<Long> sorted = new ArrayList<>(latencies);
    Collections.sort(sorted);

    long min = sorted.getFirst();
    long max = sorted.getLast();
    double avg = sorted.stream()
        .mapToLong(Long::longValue)
        .average()
        .orElse(0);

    Percentiles percentiles = calculatePercentiles(sorted);

    return new LatencyStats(min, max, avg, percentiles);
  }

  /**
   * 백분위수를 계산한다.
   */
  private Percentiles calculatePercentiles(List<Long> sorted) {
    int size = sorted.size();

    return new Percentiles(
        sorted.get(percentileIndex(size, 0.50)),
        sorted.get(percentileIndex(size, 0.90)),
        sorted.get(percentileIndex(size, 0.95)),
        sorted.get(percentileIndex(size, 0.99)),
        sorted.getFirst(),
        sorted.getLast()
    );
  }

  /**
   * 백분위수 인덱스를 계산한다.
   */
  private int percentileIndex(int size, double percentile) {
    int index = (int) Math.ceil(size * percentile) - 1;
    return Math.max(0, Math.min(index, size - 1));
  }
}