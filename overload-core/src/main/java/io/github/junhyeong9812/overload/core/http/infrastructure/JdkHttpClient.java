package io.github.junhyeong9812.overload.core.http.infrastructure;

import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult.ErrorType;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * JDK 11+ HttpClient 기반의 HTTP 클라이언트 구현체.
 *
 * <p>Java 표준 라이브러리의 {@link HttpClient}를 사용하여
 * HTTP 요청을 처리한다. 외부 의존성 없이 동작한다.
 *
 * <p><b>특징:</b>
 * <ul>
 *   <li>JDK 표준 라이브러리 사용 (외부 의존성 없음)</li>
 *   <li>HTTP/1.1 및 HTTP/2 지원</li>
 *   <li>연결 타임아웃 설정 지원</li>
 *   <li>Virtual Thread 친화적</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * HttpClientPort client = new JdkHttpClient(Duration.ofSeconds(5));
 *
 * HttpRequest request = HttpRequest.from(
 *     "https://api.example.com",
 *     HttpMethod.GET,
 *     Map.of(),
 *     null
 * );
 *
 * RequestResult result = client.send(request);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see HttpClientPort
 */
public class JdkHttpClient implements HttpClientPort {

  private final HttpClient client;
  private final Duration timeout;

  /**
   * 지정된 타임아웃으로 JdkHttpClient를 생성한다.
   *
   * @param timeout 연결 및 요청 타임아웃
   */
  public JdkHttpClient(Duration timeout) {
    this.timeout = timeout;
    this.client = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build();
  }

  /**
   * HTTP 요청을 전송하고 결과를 반환한다.
   *
   * <p>응답 본문은 무시하고 상태 코드와 지연 시간만 기록한다.
   * 부하 테스트에서는 응답 본문보다 성능 측정이 목적이기 때문이다.
   *
   * @param request 전송할 HTTP 요청
   * @return 요청 결과 - 성공 또는 실패
   */
  @Override
  public RequestResult send(HttpRequest request) {
    long startTime = System.currentTimeMillis();

    try {
      java.net.http.HttpRequest.Builder builder =
          java.net.http.HttpRequest.newBuilder()
              .uri(URI.create(request.url()))
              .timeout(timeout);

      // 헤더 추가
      request.headers().forEach(builder::header);

      // 메서드 및 바디 설정
      var bodyPublisher = request.body() != null
          ? java.net.http.HttpRequest.BodyPublishers.ofString(request.body())
          : java.net.http.HttpRequest.BodyPublishers.noBody();

      switch (request.method()) {
        case GET -> builder.GET();
        case POST -> builder.POST(bodyPublisher);
        case PUT -> builder.PUT(bodyPublisher);
        case DELETE -> builder.DELETE();
        case PATCH -> builder.method("PATCH", bodyPublisher);
        case HEAD -> builder.method("HEAD", bodyPublisher);
        case OPTIONS -> builder.method("OPTIONS", bodyPublisher);
      }

      HttpResponse<Void> response = client.send(
          builder.build(),
          HttpResponse.BodyHandlers.discarding()
      );

      long latency = System.currentTimeMillis() - startTime;
      return new RequestResult.Success(response.statusCode(), latency);

    } catch (HttpTimeoutException e) {
      long latency = System.currentTimeMillis() - startTime;
      return new RequestResult.Failure(e.getMessage(), ErrorType.TIMEOUT, latency);

    } catch (ConnectException e) {
      long latency = System.currentTimeMillis() - startTime;
      return new RequestResult.Failure(e.getMessage(), ErrorType.CONNECTION_REFUSED, latency);

    } catch (Exception e) {
      long latency = System.currentTimeMillis() - startTime;
      String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      return new RequestResult.Failure(message, ErrorType.UNKNOWN, latency);
    }
  }
}