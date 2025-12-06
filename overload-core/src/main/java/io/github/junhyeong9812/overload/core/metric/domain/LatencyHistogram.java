package io.github.junhyeong9812.overload.core.metric.domain;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 지연 시간 분포를 기록하는 히스토그램.
 *
 * <p>백분위수(Percentile) 계산을 위해 지연 시간을 버킷에 기록한다.
 * 동시성을 지원하며, 스레드 안전하게 동작한다.
 *
 * <p><b>버킷 구성:</b>
 * <ul>
 *   <li>0-1000ms: 1ms 단위 (1000개 버킷)</li>
 *   <li>1000ms 이상: 오버플로우 버킷</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * LatencyHistogram histogram = new LatencyHistogram();
 *
 * histogram.record(150);  // 150ms 기록
 * histogram.record(200);  // 200ms 기록
 *
 * long p99 = histogram.getPercentile(99);  // 99번째 백분위수
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class LatencyHistogram {

  private static final int BUCKET_COUNT = 1000;
  private static final int MAX_TRACKABLE_MS = 1000;

  private final LongAdder[] buckets;
  private final LongAdder overflowBucket;
  private final LongAdder totalCount;
  private final LongAdder totalSum;
  private final AtomicLong minValue;
  private final AtomicLong maxValue;

  /**
   * 새로운 LatencyHistogram을 생성한다.
   */
  public LatencyHistogram() {
    this.buckets = new LongAdder[BUCKET_COUNT];
    for (int i = 0; i < BUCKET_COUNT; i++) {
      buckets[i] = new LongAdder();
    }
    this.overflowBucket = new LongAdder();
    this.totalCount = new LongAdder();
    this.totalSum = new LongAdder();
    this.minValue = new AtomicLong(Long.MAX_VALUE);
    this.maxValue = new AtomicLong(Long.MIN_VALUE);
  }

  /**
   * 지연 시간을 기록한다.
   *
   * @param latencyMs 지연 시간 (밀리초)
   */
  public void record(long latencyMs) {
    totalCount.increment();
    totalSum.add(latencyMs);

    updateMin(latencyMs);
    updateMax(latencyMs);

    if (latencyMs >= MAX_TRACKABLE_MS) {
      overflowBucket.increment();
    } else if (latencyMs >= 0) {
      buckets[(int) latencyMs].increment();
    }
  }

  /**
   * 지정된 백분위수의 지연 시간을 반환한다.
   *
   * @param percentile 백분위수 (0-100)
   * @return 해당 백분위수의 지연 시간 (밀리초)
   */
  public long getPercentile(double percentile) {
    long count = totalCount.sum();
    if (count == 0) {
      return 0;
    }

    long targetCount = (long) Math.ceil(count * percentile / 100.0);
    long cumulative = 0;

    for (int i = 0; i < BUCKET_COUNT; i++) {
      cumulative += buckets[i].sum();
      if (cumulative >= targetCount) {
        return i;
      }
    }

    return MAX_TRACKABLE_MS;
  }

  /**
   * 총 기록된 샘플 수를 반환한다.
   *
   * @return 총 샘플 수
   */
  public long getCount() {
    return totalCount.sum();
  }

  /**
   * 평균 지연 시간을 반환한다.
   *
   * @return 평균 지연 시간 (밀리초), 샘플이 없으면 0.0
   */
  public double getMean() {
    long count = totalCount.sum();
    if (count == 0) {
      return 0.0;
    }
    return (double) totalSum.sum() / count;
  }

  /**
   * 최소 지연 시간을 반환한다.
   *
   * @return 최소 지연 시간 (밀리초), 샘플이 없으면 0
   */
  public long getMin() {
    long min = minValue.get();
    return min == Long.MAX_VALUE ? 0 : min;
  }

  /**
   * 최대 지연 시간을 반환한다.
   *
   * @return 최대 지연 시간 (밀리초), 샘플이 없으면 0
   */
  public long getMax() {
    long max = maxValue.get();
    return max == Long.MIN_VALUE ? 0 : max;
  }

  /**
   * 히스토그램을 초기화한다.
   */
  public void reset() {
    Arrays.stream(buckets).forEach(LongAdder::reset);
    overflowBucket.reset();
    totalCount.reset();
    totalSum.reset();
    minValue.set(Long.MAX_VALUE);
    maxValue.set(Long.MIN_VALUE);
  }

  private void updateMin(long value) {
    long current;
    while (value < (current = minValue.get())) {
      if (minValue.compareAndSet(current, value)) {
        break;
      }
    }
  }

  private void updateMax(long value) {
    long current;
    while (value > (current = maxValue.get())) {
      if (maxValue.compareAndSet(current, value)) {
        break;
      }
    }
  }
}