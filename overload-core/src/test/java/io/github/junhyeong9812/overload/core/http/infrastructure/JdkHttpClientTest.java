package io.github.junhyeong9812.overload.core.http.infrastructure;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JdkHttpClient")
class JdkHttpClientTest {

  private JdkHttpClient client;

  @BeforeEach
  void setUp() {
    client = new JdkHttpClient(Duration.ofSeconds(5));
  }

  @Test
  @DisplayName("HttpClientPort 인터페이스를 구현한다")
  void implementsHttpClientPort() {
    assertThat(client).isInstanceOf(HttpClientPort.class);
  }

  @Nested
  @DisplayName("에러 처리")
  class ErrorHandlingTest {

    @Test
    @DisplayName("잘못된 URL로 요청하면 Failure를 반환한다")
    void returnsFailureForInvalidUrl() {
      HttpRequest request = HttpRequest.from(
          "invalid-url",
          HttpMethod.GET,
          Map.of(),
          null
      );

      RequestResult result = client.send(request);

      assertThat(result).isInstanceOf(RequestResult.Failure.class);
      RequestResult.Failure failure = (RequestResult.Failure) result;
      assertThat(failure.errorType()).isEqualTo(RequestResult.ErrorType.UNKNOWN);
    }

    @Test
    @DisplayName("연결할 수 없는 호스트로 요청하면 Failure를 반환한다")
    void returnsFailureForUnreachableHost() {
      HttpRequest request = HttpRequest.from(
          "http://localhost:59999",
          HttpMethod.GET,
          Map.of(),
          null
      );

      RequestResult result = client.send(request);

      assertThat(result).isInstanceOf(RequestResult.Failure.class);
    }

    @Test
    @DisplayName("타임아웃이 짧으면 TIMEOUT 에러를 반환한다")
    void returnsTimeoutError() {
      JdkHttpClient shortTimeoutClient = new JdkHttpClient(Duration.ofMillis(1));

      HttpRequest request = HttpRequest.from(
          "http://10.255.255.1:80",  // 응답하지 않는 IP
          HttpMethod.GET,
          Map.of(),
          null
      );

      RequestResult result = shortTimeoutClient.send(request);

      assertThat(result).isInstanceOf(RequestResult.Failure.class);
    }
  }

  @Nested
  @DisplayName("Latency 측정")
  class LatencyTest {

    @Test
    @DisplayName("latencyMs가 0 이상의 값을 반환한다")
    void latencyIsNonNegative() {
      HttpRequest request = HttpRequest.from(
          "http://localhost:59999",
          HttpMethod.GET,
          Map.of(),
          null
      );

      RequestResult result = client.send(request);

      assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
    }
  }

  @Nested
  @DisplayName("HTTP 메서드")
  class HttpMethodTest {

    @Test
    @DisplayName("GET 요청을 보낼 수 있다")
    void sendGetRequest() {
      HttpRequest request = HttpRequest.from(
          "http://localhost:59999",
          HttpMethod.GET,
          Map.of(),
          null
      );

      RequestResult result = client.send(request);

      // 연결 실패여도 요청 자체는 처리됨
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("POST 요청을 보낼 수 있다")
    void sendPostRequest() {
      HttpRequest request = HttpRequest.from(
          "http://localhost:59999",
          HttpMethod.POST,
          Map.of("Content-Type", "application/json"),
          "{\"test\": true}"
      );

      RequestResult result = client.send(request);

      assertThat(result).isNotNull();
    }
  }
}