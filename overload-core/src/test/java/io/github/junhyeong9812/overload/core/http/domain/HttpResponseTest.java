package io.github.junhyeong9812.overload.core.http.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpResponse")
class HttpResponseTest {

  @Nested
  @DisplayName("isSuccess")
  class IsSuccessTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 299})
    @DisplayName("2xx 상태 코드는 true를 반환한다")
    void returnsTrueFor2xx(int statusCode) {
      HttpResponse response = new HttpResponse(statusCode, Map.of(), null);

      assertThat(response.isSuccess()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {199, 300, 400, 500})
    @DisplayName("2xx 범위가 아니면 false를 반환한다")
    void returnsFalseForNon2xx(int statusCode) {
      HttpResponse response = new HttpResponse(statusCode, Map.of(), null);

      assertThat(response.isSuccess()).isFalse();
    }
  }

  @Nested
  @DisplayName("isClientError")
  class IsClientErrorTest {

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 499})
    @DisplayName("4xx 상태 코드는 true를 반환한다")
    void returnsTrueFor4xx(int statusCode) {
      HttpResponse response = new HttpResponse(statusCode, Map.of(), null);

      assertThat(response.isClientError()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 399, 500})
    @DisplayName("4xx 범위가 아니면 false를 반환한다")
    void returnsFalseForNon4xx(int statusCode) {
      HttpResponse response = new HttpResponse(statusCode, Map.of(), null);

      assertThat(response.isClientError()).isFalse();
    }
  }

  @Nested
  @DisplayName("isServerError")
  class IsServerErrorTest {

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 599})
    @DisplayName("5xx 상태 코드는 true를 반환한다")
    void returnsTrueFor5xx(int statusCode) {
      HttpResponse response = new HttpResponse(statusCode, Map.of(), null);

      assertThat(response.isServerError()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 499})
    @DisplayName("5xx 범위가 아니면 false를 반환한다")
    void returnsFalseForNon5xx(int statusCode) {
      HttpResponse response = new HttpResponse(statusCode, Map.of(), null);

      assertThat(response.isServerError()).isFalse();
    }
  }

  @Test
  @DisplayName("모든 필드를 포함하여 생성할 수 있다")
  void createWithAllFields() {
    HttpResponse response = new HttpResponse(
        200,
        Map.of("Content-Type", "application/json"),
        "{\"status\": \"ok\"}"
    );

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers()).containsEntry("Content-Type", "application/json");
    assertThat(response.body()).isEqualTo("{\"status\": \"ok\"}");
  }
}