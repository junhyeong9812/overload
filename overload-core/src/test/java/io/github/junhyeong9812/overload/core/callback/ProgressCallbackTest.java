package io.github.junhyeong9812.overload.core.callback;

import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link ProgressCallback} 테스트.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@DisplayName("ProgressCallback")
class ProgressCallbackTest {

  /** 테스트용 더미 RequestResult */
  private static final RequestResult DUMMY_RESULT = new RequestResult.Success(200, 10);

  @Nested
  @DisplayName("noop")
  class NoopTest {

    @Test
    @DisplayName("아무 동작도 하지 않는 콜백을 반환한다")
    void returnsNoopCallback() {
      ProgressCallback callback = ProgressCallback.noop();

      // 예외 없이 호출됨
      callback.onProgress(50, 100, DUMMY_RESULT);
    }

    @Test
    @DisplayName("여러 번 호출해도 예외가 발생하지 않는다")
    void canBeCalledMultipleTimes() {
      ProgressCallback callback = ProgressCallback.noop();

      callback.onProgress(0, 100, DUMMY_RESULT);
      callback.onProgress(50, 100, DUMMY_RESULT);
      callback.onProgress(100, 100, DUMMY_RESULT);

      // 예외 없이 완료
    }
  }

  @Nested
  @DisplayName("getPercentage")
  class GetPercentageTest {

    @Test
    @DisplayName("백분율을 올바르게 계산한다")
    void calculatesPercentage() {
      ProgressCallback callback = (completed, total, result) -> {};

      assertThat(callback.getPercentage(50, 100)).isCloseTo(50.0, within(0.01));
      assertThat(callback.getPercentage(25, 100)).isCloseTo(25.0, within(0.01));
      assertThat(callback.getPercentage(100, 100)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("total이 0이면 0을 반환한다")
    void returnsZeroWhenTotalIsZero() {
      ProgressCallback callback = (completed, total, result) -> {};

      assertThat(callback.getPercentage(0, 0)).isZero();
    }
  }

  @Nested
  @DisplayName("simple")
  class SimpleTest {

    @Test
    @DisplayName("간단한 콜백을 ProgressCallback으로 변환한다")
    void convertsSimpleCallback() {
      AtomicInteger lastCompleted = new AtomicInteger(-1);
      AtomicInteger lastTotal = new AtomicInteger(-1);

      ProgressCallback callback = ProgressCallback.simple((completed, total) -> {
        lastCompleted.set(completed);
        lastTotal.set(total);
      });

      callback.onProgress(50, 100, DUMMY_RESULT);

      assertThat(lastCompleted.get()).isEqualTo(50);
      assertThat(lastTotal.get()).isEqualTo(100);
    }

    @Test
    @DisplayName("RequestResult는 무시된다")
    void ignoresRequestResult() {
      AtomicInteger callCount = new AtomicInteger(0);

      ProgressCallback callback = ProgressCallback.simple((completed, total) ->
          callCount.incrementAndGet()
      );

      callback.onProgress(1, 100, new RequestResult.Success(200, 10));
      callback.onProgress(2, 100, new RequestResult.Failure("error", RequestResult.ErrorType.TIMEOUT, 100));

      assertThat(callCount.get()).isEqualTo(2);
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

      ProgressCallback callback = (completed, total, result) -> {
        lastCompleted.set(completed);
        lastTotal.set(total);
      };

      callback.onProgress(50, 100, DUMMY_RESULT);

      assertThat(lastCompleted.get()).isEqualTo(50);
      assertThat(lastTotal.get()).isEqualTo(100);
    }

    @Test
    @DisplayName("RequestResult에 접근할 수 있다")
    void canAccessRequestResult() {
      AtomicInteger capturedStatusCode = new AtomicInteger(-1);

      ProgressCallback callback = (completed, total, result) -> {
        if (result instanceof RequestResult.Success success) {
          capturedStatusCode.set(success.statusCode());
        }
      };

      callback.onProgress(1, 100, new RequestResult.Success(201, 10));

      assertThat(capturedStatusCode.get()).isEqualTo(201);
    }
  }
}