package io.github.junhyeong9812.overload.core.engine.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ExecutionContext")
class ExecutionContextTest {

  private ExecutionContext context;

  @BeforeEach
  void setUp() {
    context = new ExecutionContext(100);
  }

  @Test
  @DisplayName("총 요청 수를 반환한다")
  void returnsTotalRequests() {
    assertThat(context.getTotalRequests()).isEqualTo(100);
  }

  @Test
  @DisplayName("초기 완료 수는 0이다")
  void initialCompletedCountIsZero() {
    assertThat(context.getCompletedCount()).isZero();
  }

  @Nested
  @DisplayName("incrementAndGetCompleted")
  class IncrementTest {

    @Test
    @DisplayName("완료 수를 증가시키고 반환한다")
    void incrementsAndReturns() {
      int result = context.incrementAndGetCompleted();

      assertThat(result).isEqualTo(1);
      assertThat(context.getCompletedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 번 증가시킬 수 있다")
    void incrementsMultipleTimes() {
      context.incrementAndGetCompleted();
      context.incrementAndGetCompleted();
      int result = context.incrementAndGetCompleted();

      assertThat(result).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("cancel")
  class CancelTest {

    @Test
    @DisplayName("초기 상태는 취소되지 않음")
    void initiallyNotCancelled() {
      assertThat(context.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("cancel() 호출 후 취소됨")
    void cancelledAfterCancel() {
      context.cancel();

      assertThat(context.isCancelled()).isTrue();
    }
  }

  @Nested
  @DisplayName("getProgress")
  class GetProgressTest {

    @Test
    @DisplayName("초기 진행률은 0%")
    void initialProgressIsZero() {
      assertThat(context.getProgress()).isZero();
    }

    @Test
    @DisplayName("진행률을 올바르게 계산한다")
    void calculatesProgress() {
      for (int i = 0; i < 50; i++) {
        context.incrementAndGetCompleted();
      }

      assertThat(context.getProgress()).isCloseTo(50.0, within(0.01));
    }

    @Test
    @DisplayName("100% 완료 시 100을 반환한다")
    void returns100WhenComplete() {
      for (int i = 0; i < 100; i++) {
        context.incrementAndGetCompleted();
      }

      assertThat(context.getProgress()).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("totalRequests가 0이면 0을 반환한다")
    void returnsZeroWhenTotalIsZero() {
      ExecutionContext emptyContext = new ExecutionContext(0);

      assertThat(emptyContext.getProgress()).isZero();
    }
  }

  @Nested
  @DisplayName("동시성")
  class ConcurrencyTest {

    @Test
    @DisplayName("여러 스레드에서 동시에 증가시킬 수 있다")
    void concurrentIncrement() throws InterruptedException {
      int threadCount = 10;
      int incrementsPerThread = 10;
      Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        threads[i] = new Thread(() -> {
          for (int j = 0; j < incrementsPerThread; j++) {
            context.incrementAndGetCompleted();
          }
        });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      assertThat(context.getCompletedCount()).isEqualTo(threadCount * incrementsPerThread);
    }
  }
}