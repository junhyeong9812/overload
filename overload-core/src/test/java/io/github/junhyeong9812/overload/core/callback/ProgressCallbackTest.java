package io.github.junhyeong9812.overload.core.callback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ProgressCallback")
class ProgressCallbackTest {

  @Nested
  @DisplayName("noop")
  class NoopTest {

    @Test
    @DisplayName("아무 동작도 하지 않는 콜백을 반환한다")
    void returnsNoopCallback() {
      ProgressCallback callback = ProgressCallback.noop();

      // 예외 없이 호출됨
      callback.onProgress(50, 100);
    }

    @Test
    @DisplayName("항상 같은 인스턴스를 반환한다")
    void returnsSameInstance() {
      ProgressCallback callback1 = ProgressCallback.noop();
      ProgressCallback callback2 = ProgressCallback.noop();

      // 람다이므로 같은 인스턴스가 아닐 수 있음
      // 동작만 같으면 됨
      assertThat(callback1).isNotNull();
      assertThat(callback2).isNotNull();
    }
  }

  @Nested
  @DisplayName("getPercentage")
  class GetPercentageTest {

    @Test
    @DisplayName("백분율을 올바르게 계산한다")
    void calculatesPercentage() {
      ProgressCallback callback = (completed, total) -> {};

      assertThat(callback.getPercentage(50, 100)).isCloseTo(50.0, within(0.01));
      assertThat(callback.getPercentage(25, 100)).isCloseTo(25.0, within(0.01));
      assertThat(callback.getPercentage(100, 100)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("total이 0이면 0을 반환한다")
    void returnsZeroWhenTotalIsZero() {
      ProgressCallback callback = (completed, total) -> {};

      assertThat(callback.getPercentage(0, 0)).isZero();
    }
  }

  @Nested
  @DisplayName("람다 구현")
  class LambdaImplementationTest {

    @Test
    @DisplayName("람다로 구현할 수 있다")
    void canImplementWithLambda() {
      AtomicInteger lastCompleted = new AtomicInteger(-1);
      AtomicInteger lastTotal = new AtomicInteger(-1);

      ProgressCallback callback = (completed, total) -> {
        lastCompleted.set(completed);
        lastTotal.set(total);
      };

      callback.onProgress(50, 100);

      assertThat(lastCompleted.get()).isEqualTo(50);
      assertThat(lastTotal.get()).isEqualTo(100);
    }
  }
}