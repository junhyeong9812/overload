package io.github.junhyeong9812.overload.core.metric.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("LatencyHistogram")
class LatencyHistogramTest {

  private LatencyHistogram histogram;

  @BeforeEach
  void setUp() {
    histogram = new LatencyHistogram();
  }

  @Nested
  @DisplayName("record")
  class RecordTest {

    @Test
    @DisplayName("지연 시간을 기록할 수 있다")
    void recordLatency() {
      histogram.record(100);

      assertThat(histogram.getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 지연 시간을 기록할 수 있다")
    void recordMultipleLatencies() {
      histogram.record(100);
      histogram.record(200);
      histogram.record(300);

      assertThat(histogram.getCount()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("getMean")
  class GetMeanTest {

    @Test
    @DisplayName("평균을 올바르게 계산한다")
    void calculatesMean() {
      histogram.record(100);
      histogram.record(200);
      histogram.record(300);

      assertThat(histogram.getMean()).isCloseTo(200.0, within(0.01));
    }

    @Test
    @DisplayName("샘플이 없으면 0을 반환한다")
    void returnsZeroWhenEmpty() {
      assertThat(histogram.getMean()).isZero();
    }
  }

  @Nested
  @DisplayName("getMin/getMax")
  class MinMaxTest {

    @Test
    @DisplayName("최소값을 올바르게 반환한다")
    void returnsMin() {
      histogram.record(100);
      histogram.record(50);
      histogram.record(200);

      assertThat(histogram.getMin()).isEqualTo(50);
    }

    @Test
    @DisplayName("최대값을 올바르게 반환한다")
    void returnsMax() {
      histogram.record(100);
      histogram.record(50);
      histogram.record(200);

      assertThat(histogram.getMax()).isEqualTo(200);
    }

    @Test
    @DisplayName("샘플이 없으면 0을 반환한다")
    void returnsZeroWhenEmpty() {
      assertThat(histogram.getMin()).isZero();
      assertThat(histogram.getMax()).isZero();
    }
  }

  @Nested
  @DisplayName("getPercentile")
  class GetPercentileTest {

    @Test
    @DisplayName("P50을 올바르게 계산한다")
    void calculatesP50() {
      for (int i = 1; i <= 100; i++) {
        histogram.record(i);
      }

      long p50 = histogram.getPercentile(50);
      assertThat(p50).isBetween(49L, 51L);
    }

    @Test
    @DisplayName("P99를 올바르게 계산한다")
    void calculatesP99() {
      for (int i = 1; i <= 100; i++) {
        histogram.record(i);
      }

      long p99 = histogram.getPercentile(99);
      assertThat(p99).isBetween(98L, 100L);
    }

    @Test
    @DisplayName("샘플이 없으면 0을 반환한다")
    void returnsZeroWhenEmpty() {
      assertThat(histogram.getPercentile(50)).isZero();
    }
  }

  @Nested
  @DisplayName("reset")
  class ResetTest {

    @Test
    @DisplayName("모든 값을 초기화한다")
    void resetsAllValues() {
      histogram.record(100);
      histogram.record(200);

      histogram.reset();

      assertThat(histogram.getCount()).isZero();
      assertThat(histogram.getMean()).isZero();
      assertThat(histogram.getMin()).isZero();
      assertThat(histogram.getMax()).isZero();
    }
  }

  @Nested
  @DisplayName("동시성")
  class ConcurrencyTest {

    @Test
    @DisplayName("여러 스레드에서 동시에 기록할 수 있다")
    void concurrentRecording() throws InterruptedException {
      int threadCount = 10;
      int recordsPerThread = 1000;
      Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        threads[i] = new Thread(() -> {
          for (int j = 0; j < recordsPerThread; j++) {
            histogram.record(j % 100);
          }
        });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      assertThat(histogram.getCount()).isEqualTo(threadCount * recordsPerThread);
    }
  }
}