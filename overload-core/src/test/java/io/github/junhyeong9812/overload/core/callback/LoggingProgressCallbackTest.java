package io.github.junhyeong9812.overload.core.callback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoggingProgressCallback")
class LoggingProgressCallbackTest {

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
      callback.onProgress(10, 100);
      callback.onProgress(50, 100);
      callback.onProgress(100, 100);
    }

    @Test
    @DisplayName("total이 0이면 아무 동작 안함")
    void handlesZeroTotal() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      // 예외 없이 호출됨
      callback.onProgress(0, 0);
    }

    @Test
    @DisplayName("음수 total도 처리한다")
    void handlesNegativeTotal() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      // 예외 없이 호출됨
      callback.onProgress(0, -1);
    }
  }

  @Nested
  @DisplayName("reset")
  class ResetTest {

    @Test
    @DisplayName("reset 후 다시 로깅할 수 있다")
    void canLogAfterReset() {
      LoggingProgressCallback callback = new LoggingProgressCallback();

      callback.onProgress(100, 100);
      callback.reset();
      callback.onProgress(10, 100);

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