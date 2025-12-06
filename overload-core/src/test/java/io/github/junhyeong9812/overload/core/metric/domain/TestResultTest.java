package io.github.junhyeong9812.overload.core.metric.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TestResult")
class TestResultTest {

  @Nested
  @DisplayName("successRate")
  class SuccessRateTest {

    @Test
    @DisplayName("성공률을 올바르게 계산한다")
    void calculatesSuccessRate() {
      TestResult result = new TestResult(
          100, 80, 20,
          Duration.ofSeconds(10),
          10.0,
          TestResult.LatencyStats.empty()
      );

      assertThat(result.successRate()).isCloseTo(80.0, within(0.01));
    }

    @Test
    @DisplayName("전체 요청이 0이면 0을 반환한다")
    void returnsZeroWhenNoRequests() {
      TestResult result = new TestResult(
          0, 0, 0,
          Duration.ofSeconds(10),
          0.0,
          TestResult.LatencyStats.empty()
      );

      assertThat(result.successRate()).isZero();
    }

    @Test
    @DisplayName("100% 성공률을 계산한다")
    void calculates100PercentSuccess() {
      TestResult result = new TestResult(
          100, 100, 0,
          Duration.ofSeconds(10),
          10.0,
          TestResult.LatencyStats.empty()
      );

      assertThat(result.successRate()).isCloseTo(100.0, within(0.01));
    }
  }

  @Nested
  @DisplayName("failRate")
  class FailRateTest {

    @Test
    @DisplayName("실패율을 올바르게 계산한다")
    void calculatesFailRate() {
      TestResult result = new TestResult(
          100, 80, 20,
          Duration.ofSeconds(10),
          10.0,
          TestResult.LatencyStats.empty()
      );

      assertThat(result.failRate()).isCloseTo(20.0, within(0.01));
    }

    @Test
    @DisplayName("성공률과 실패율의 합은 100이다")
    void successAndFailRateSumTo100() {
      TestResult result = new TestResult(
          100, 75, 25,
          Duration.ofSeconds(10),
          10.0,
          TestResult.LatencyStats.empty()
      );

      assertThat(result.successRate() + result.failRate()).isCloseTo(100.0, within(0.01));
    }
  }

  @Nested
  @DisplayName("LatencyStats")
  class LatencyStatsTest {

    @Test
    @DisplayName("empty()는 모든 값이 0인 LatencyStats를 반환한다")
    void emptyReturnsZeroValues() {
      TestResult.LatencyStats empty = TestResult.LatencyStats.empty();

      assertThat(empty.min()).isZero();
      assertThat(empty.max()).isZero();
      assertThat(empty.avg()).isZero();
      assertThat(empty.percentiles()).isEqualTo(Percentiles.empty());
    }

    @Test
    @DisplayName("모든 값으로 생성할 수 있다")
    void createWithAllValues() {
      Percentiles percentiles = new Percentiles(50, 90, 95, 99, 10, 200);
      TestResult.LatencyStats stats = new TestResult.LatencyStats(10, 200, 75.5, percentiles);

      assertThat(stats.min()).isEqualTo(10);
      assertThat(stats.max()).isEqualTo(200);
      assertThat(stats.avg()).isEqualTo(75.5);
      assertThat(stats.percentiles()).isEqualTo(percentiles);
    }
  }

  @Test
  @DisplayName("모든 필드로 TestResult를 생성할 수 있다")
  void createWithAllFields() {
    Percentiles percentiles = new Percentiles(50, 90, 95, 99, 10, 200);
    TestResult.LatencyStats latencyStats = new TestResult.LatencyStats(10, 200, 75.5, percentiles);

    TestResult result = new TestResult(
        1000, 950, 50,
        Duration.ofSeconds(10),
        100.0,
        latencyStats
    );

    assertThat(result.totalRequests()).isEqualTo(1000);
    assertThat(result.successCount()).isEqualTo(950);
    assertThat(result.failCount()).isEqualTo(50);
    assertThat(result.totalDuration()).isEqualTo(Duration.ofSeconds(10));
    assertThat(result.requestsPerSecond()).isEqualTo(100.0);
    assertThat(result.latencyStats()).isEqualTo(latencyStats);
  }
}