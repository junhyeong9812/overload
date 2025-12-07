package io.github.junhyeong9812.overload.core.callback;

import io.github.junhyeong9812.overload.core.http.domain.ErrorType;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LoggingProgressCallback} 테스트.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@DisplayName("LoggingProgressCallback")
class LoggingProgressCallbackTest {

  /** 테스트용 더미 RequestResult */
  private static final RequestResult DUMMY_RESULT = new RequestResult.Success(200, 10);

  @Nested
  @DisplayName("생성자")
  class ConstructorTest {

    @Test
    @DisplayName("기본 생성자로 생성할 수 있다")
    void createWithDefaultConstructor() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      assertThat(callback).isNotNull();
    }

    @Test
    @DisplayName("로깅 간격을 지정하여 생성할 수 있다")
    void createWithInterval() {
      LoggingProgressCallback callback = new LoggingProgressCallback(5);

      assertThat(callback).isNotNull();
    }

    @Test
    @DisplayName("간격이 0이면 예외 발생")
    void throwsWhenIntervalIsZero() {
      assertThatThrownBy(() -> new LoggingProgressCallback(0))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("간격이 101이면 예외 발생")
    void throwsWhenIntervalIs101() {
      assertThatThrownBy(() -> new LoggingProgressCallback(101))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("간격이 1이면 정상")
    void acceptsIntervalOf1() {
      LoggingProgressCallback callback = new LoggingProgressCallback(1);

      assertThat(callback).isNotNull();
    }

    @Test
    @DisplayName("간격이 100이면 정상")
    void acceptsIntervalOf100() {
      LoggingProgressCallback callback = new LoggingProgressCallback(100);

      assertThat(callback).isNotNull();
    }
  }

  @Nested
  @DisplayName("onProgress")
  class OnProgressTest {

    @Test
    @DisplayName("예외 없이 호출된다")
    void callsWithoutException() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      // 예외 없이 호출됨
      callback.onProgress(10, 100, DUMMY_RESULT);
      callback.onProgress(50, 100, DUMMY_RESULT);
      callback.onProgress(100, 100, DUMMY_RESULT);
    }

    @Test
    @DisplayName("total이 0이면 아무 동작 안함")
    void handlesZeroTotal() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      // 예외 없이 호출됨
      callback.onProgress(0, 0, DUMMY_RESULT);
    }

    @Test
    @DisplayName("음수 total도 처리한다")
    void handlesNegativeTotal() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      // 예외 없이 호출됨
      callback.onProgress(0, -1, DUMMY_RESULT);
    }

    @Test
    @DisplayName("Success 결과와 함께 호출할 수 있다")
    void worksWithSuccessResult() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      callback.onProgress(50, 100, new RequestResult.Success(200, 25));

      // 예외 없이 완료
    }

    @Test
    @DisplayName("Failure 결과와 함께 호출할 수 있다")
    void worksWithFailureResult() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      callback.onProgress(50, 100, new RequestResult.Failure(
          "Connection refused",
          ErrorType.CONNECTION_REFUSED,
          100
      ));

      // 예외 없이 완료
    }
  }

  @Nested
  @DisplayName("reset")
  class ResetTest {

    @Test
    @DisplayName("reset 후 다시 로깅할 수 있다")
    void canLogAfterReset() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      callback.onProgress(100, 100, DUMMY_RESULT);
      callback.reset();
      callback.onProgress(10, 100, DUMMY_RESULT);

      // 예외 없이 완료
    }
  }

  @Test
  @DisplayName("ProgressCallback 인터페이스를 구현한다")
  void implementsProgressCallback() {
    LoggingProgressCallback callback = new LoggingProgressCallback();

    assertThat(callback).isInstanceOf(ProgressCallback.class);
  }
}