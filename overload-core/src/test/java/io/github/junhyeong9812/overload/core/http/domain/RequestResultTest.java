package io.github.junhyeong9812.overload.core.http.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestResult")
class RequestResultTest {

  @Nested
  @DisplayName("Success")
  class SuccessTest {

    @Test
    @DisplayName("상태 코드와 지연 시간으로 생성할 수 있다")
    void createSuccess() {
      RequestResult.Success success = new RequestResult.Success(200, 150);

      assertThat(success.statusCode()).isEqualTo(200);
      assertThat(success.latencyMs()).isEqualTo(150);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 299})
    @DisplayName("2xx 상태 코드에서 isHttpSuccess()는 true를 반환한다")
    void isHttpSuccessReturnsTrue(int statusCode) {
      RequestResult.Success success = new RequestResult.Success(statusCode, 100);

      assertThat(success.isHttpSuccess()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    @DisplayName("2xx가 아닌 상태 코드에서 isHttpSuccess()는 false를 반환한다")
    void isHttpSuccessReturnsFalse(int statusCode) {
      RequestResult.Success success = new RequestResult.Success(statusCode, 100);

      assertThat(success.isHttpSuccess()).isFalse();
    }

    @Test
    @DisplayName("RequestResult 인터페이스를 구현한다")
    void implementsRequestResult() {
      RequestResult result = new RequestResult.Success(200, 100);

      assertThat(result).isInstanceOf(RequestResult.class);
      assertThat(result.latencyMs()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Failure")
  class FailureTest {

    @Test
    @DisplayName("오류 정보로 생성할 수 있다")
    void createFailure() {
      RequestResult.Failure failure = new RequestResult.Failure(
          "Connection timed out",
          RequestResult.ErrorType.TIMEOUT,
          5000
      );

      assertThat(failure.errorMessage()).isEqualTo("Connection timed out");
      assertThat(failure.errorType()).isEqualTo(RequestResult.ErrorType.TIMEOUT);
      assertThat(failure.latencyMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("RequestResult 인터페이스를 구현한다")
    void implementsRequestResult() {
      RequestResult result = new RequestResult.Failure(
          "error",
          RequestResult.ErrorType.UNKNOWN,
          100
      );

      assertThat(result).isInstanceOf(RequestResult.class);
      assertThat(result.latencyMs()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("ErrorType")
  class ErrorTypeTest {

    @Test
    @DisplayName("모든 오류 타입이 정의되어 있다")
    void allErrorTypesDefined() {
      RequestResult.ErrorType[] types = RequestResult.ErrorType.values();

      assertThat(types).containsExactly(
          RequestResult.ErrorType.TIMEOUT,
          RequestResult.ErrorType.CONNECTION_REFUSED,
          RequestResult.ErrorType.CONNECTION_RESET,
          RequestResult.ErrorType.UNKNOWN
      );
    }
  }

  @Nested
  @DisplayName("Pattern Matching")
  class PatternMatchingTest {

    @Test
    @DisplayName("switch 표현식으로 패턴 매칭할 수 있다")
    void patternMatching() {
      RequestResult success = new RequestResult.Success(200, 100);
      RequestResult failure = new RequestResult.Failure(
          "timeout",
          RequestResult.ErrorType.TIMEOUT,
          5000
      );

      String successResult = switch (success) {
        case RequestResult.Success s -> "Status: " + s.statusCode();
        case RequestResult.Failure f -> "Error: " + f.errorType();
      };

      String failureResult = switch (failure) {
        case RequestResult.Success s -> "Status: " + s.statusCode();
        case RequestResult.Failure f -> "Error: " + f.errorType();
      };

      assertThat(successResult).isEqualTo("Status: 200");
      assertThat(failureResult).isEqualTo("Error: TIMEOUT");
    }
  }
}