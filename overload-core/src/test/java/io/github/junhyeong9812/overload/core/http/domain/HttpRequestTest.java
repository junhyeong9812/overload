package io.github.junhyeong9812.overload.core.http.domain;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpRequest")
class HttpRequestTest {

  @Test
  @DisplayName("정적 팩토리 메서드로 생성할 수 있다")
  void createWithFactoryMethod() {
    HttpRequest request = HttpRequest.from(
        "https://api.example.com",
        HttpMethod.GET,
        Map.of("Accept", "application/json"),
        null
    );

    assertThat(request.url()).isEqualTo("https://api.example.com");
    assertThat(request.method()).isEqualTo(HttpMethod.GET);
    assertThat(request.headers()).containsEntry("Accept", "application/json");
    assertThat(request.body()).isNull();
  }

  @Test
  @DisplayName("body를 포함하여 생성할 수 있다")
  void createWithBody() {
    String body = "{\"name\": \"test\"}";

    HttpRequest request = HttpRequest.from(
        "https://api.example.com/users",
        HttpMethod.POST,
        Map.of("Content-Type", "application/json"),
        body
    );

    assertThat(request.body()).isEqualTo(body);
  }

  @Test
  @DisplayName("headers는 불변이다")
  void headersAreImmutable() {
    Map<String, String> mutableHeaders = new HashMap<>();
    mutableHeaders.put("Key", "Value");

    HttpRequest request = HttpRequest.from(
        "https://api.example.com",
        HttpMethod.GET,
        mutableHeaders,
        null
    );

    // 원본 맵 수정
    mutableHeaders.put("New", "Header");

    // request의 headers는 영향 없음
    assertThat(request.headers()).doesNotContainKey("New");
  }

  @Test
  @DisplayName("headers 맵 수정 시 예외 발생")
  void headersModificationThrows() {
    HttpRequest request = HttpRequest.from(
        "https://api.example.com",
        HttpMethod.GET,
        Map.of("Key", "Value"),
        null
    );

    assertThatThrownBy(() -> request.headers().put("New", "Header"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("record이므로 equals/hashCode가 값 기반이다")
  void equalsAndHashCode() {
    HttpRequest request1 = HttpRequest.from(
        "https://api.example.com",
        HttpMethod.GET,
        Map.of(),
        null
    );

    HttpRequest request2 = HttpRequest.from(
        "https://api.example.com",
        HttpMethod.GET,
        Map.of(),
        null
    );

    assertThat(request1).isEqualTo(request2);
    assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
  }
}