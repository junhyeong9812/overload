package io.github.junhyeong9812.overload.core.metric.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Percentiles")
class PercentilesTest {

  @Test
  @DisplayName("모든 백분위수 값으로 생성할 수 있다")
  void createWithAllValues() {
    Percentiles percentiles = new Percentiles(50, 90, 95, 99, 10, 200);

    assertThat(percentiles.p50()).isEqualTo(50);
    assertThat(percentiles.p90()).isEqualTo(90);
    assertThat(percentiles.p95()).isEqualTo(95);
    assertThat(percentiles.p99()).isEqualTo(99);
    assertThat(percentiles.min()).isEqualTo(10);
    assertThat(percentiles.max()).isEqualTo(200);
  }

  @Test
  @DisplayName("empty()는 모든 값이 0인 Percentiles를 반환한다")
  void emptyReturnsZeroValues() {
    Percentiles empty = Percentiles.empty();

    assertThat(empty.p50()).isZero();
    assertThat(empty.p90()).isZero();
    assertThat(empty.p95()).isZero();
    assertThat(empty.p99()).isZero();
    assertThat(empty.min()).isZero();
    assertThat(empty.max()).isZero();
  }

  @Test
  @DisplayName("record이므로 equals/hashCode가 값 기반이다")
  void equalsAndHashCode() {
    Percentiles p1 = new Percentiles(50, 90, 95, 99, 10, 200);
    Percentiles p2 = new Percentiles(50, 90, 95, 99, 10, 200);

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }
}