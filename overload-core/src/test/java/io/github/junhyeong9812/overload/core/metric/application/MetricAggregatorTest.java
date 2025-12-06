package io.github.junhyeong9812.overload.core.metric.application;

import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("MetricAggregator")
class MetricAggregatorTest {

  private MetricAggregator aggregator;

  @BeforeEach
  void setUp() {
    aggregator = new MetricAggregator();
  }

  @Nested
  @DisplayName("record")
  class RecordTest {

    @Test
    @DisplayName("Success 결과를 기록할 수 있다")
    void recordSuccess() {
      aggregator.start();
      aggregator.record(new RequestResult.Success(200, 100));
      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.totalRequests()).isEqualTo(1);
      assertThat(result.successCount()).isEqualTo(1);
      assertThat(result.failCount()).isZero();
    }

    @Test
    @DisplayName("Failure 결과를 기록할 수 있다")
    void recordFailure() {
      aggregator.start();
      aggregator.record(new RequestResult.Failure(
          "timeout",
          RequestResult.ErrorType.TIMEOUT,
          5000
      ));
      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.totalRequests()).isEqualTo(1);
      assertThat(result.successCount()).isZero();
      assertThat(result.failCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("HTTP 4xx/5xx Success는 실패로 카운트된다")
    void httpErrorSuccessCountedAsFail() {
      aggregator.start();
      aggregator.record(new RequestResult.Success(500, 100));
      aggregator.record(new RequestResult.Success(404, 50));
      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.totalRequests()).isEqualTo(2);
      assertThat(result.successCount()).isZero();
      assertThat(result.failCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 결과를 기록할 수 있다")
    void recordMultipleResults() {
      aggregator.start();

      for (int i = 0; i < 10; i++) {
        aggregator.record(new RequestResult.Success(200, 100 + i));
      }

      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.totalRequests()).isEqualTo(10);
      assertThat(result.successCount()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("aggregate")
  class AggregateTest {

    @Test
    @DisplayName("RPS를 올바르게 계산한다")
    void calculatesRps() {
      aggregator.start();

      for (int i = 0; i < 100; i++) {
        aggregator.record(new RequestResult.Success(200, 10));
      }

      // 약간의 시간이 지난 후
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.requestsPerSecond()).isGreaterThan(0);
    }

    @Test
    @DisplayName("지연 시간 통계를 올바르게 계산한다")
    void calculatesLatencyStats() {
      aggregator.start();

      aggregator.record(new RequestResult.Success(200, 50));
      aggregator.record(new RequestResult.Success(200, 100));
      aggregator.record(new RequestResult.Success(200, 150));

      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.latencyStats().min()).isEqualTo(50);
      assertThat(result.latencyStats().max()).isEqualTo(150);
      assertThat(result.latencyStats().avg()).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("백분위수를 올바르게 계산한다")
    void calculatesPercentiles() {
      aggregator.start();

      for (int i = 1; i <= 100; i++) {
        aggregator.record(new RequestResult.Success(200, i));
      }

      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.latencyStats().percentiles().p50()).isBetween(49L, 51L);
      assertThat(result.latencyStats().percentiles().p90()).isBetween(89L, 91L);
      assertThat(result.latencyStats().percentiles().p99()).isBetween(98L, 100L);
    }

    @Test
    @DisplayName("결과가 없으면 빈 통계를 반환한다")
    void returnsEmptyStatsWhenNoResults() {
      aggregator.start();
      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.totalRequests()).isZero();
      assertThat(result.latencyStats()).isEqualTo(TestResult.LatencyStats.empty());
    }
  }

  @Nested
  @DisplayName("동시성")
  class ConcurrencyTest {

    @Test
    @DisplayName("여러 스레드에서 동시에 기록할 수 있다")
    void concurrentRecording() throws InterruptedException {
      int threadCount = 10;
      int recordsPerThread = 100;
      Thread[] threads = new Thread[threadCount];

      aggregator.start();

      for (int i = 0; i < threadCount; i++) {
        threads[i] = new Thread(() -> {
          for (int j = 0; j < recordsPerThread; j++) {
            aggregator.record(new RequestResult.Success(200, j));
          }
        });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      aggregator.end();

      TestResult result = aggregator.aggregate();

      assertThat(result.totalRequests()).isEqualTo(threadCount * recordsPerThread);
    }
  }
}