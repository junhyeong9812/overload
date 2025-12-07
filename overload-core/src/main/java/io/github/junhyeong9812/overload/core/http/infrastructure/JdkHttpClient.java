package io.github.junhyeong9812.overload.core.http.infrastructure;

import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.http.domain.ErrorType;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * JDK HttpClient 기반의 HTTP 클라이언트 구현체.
 *
 * <p>Java 표준 라이브러리의 {@link HttpClient}를 사용하여
 * HTTP 요청을 처리한다. 외부 의존성 없이 동작한다.
 *
 * <p><b>특징:</b>
 * <ul>
 *   <li>JDK 표준 라이브러리 사용 (외부 의존성 없음)</li>
 *   <li>HTTP/1.1 사용 (부하 테스트를 위한 실제 동시 연결)</li>
 *   <li>Virtual Thread 친화적 Executor 사용</li>
 *   <li>나노초 단위 정밀 지연 시간 측정</li>
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
   * <p>다음과 같은 최적화 설정이 적용된다:
   * <ul>
   *   <li>HTTP/1.1 - 실제 동시 연결 테스트를 위해 multiplexing 비활성화</li>
   *   <li>Virtual Thread Executor - 높은 동시성 지원</li>
   *   <li>리다이렉트 비활성화 - 정확한 지연 시간 측정</li>
   * </ul>
   *
   * @param timeout 연결 및 요청 타임아웃
   */
  public JdkHttpClient(Duration timeout) {
    this.timeout = timeout;
    this.client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(timeout)
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  /**
   * HTTP 요청을 전송하고 결과를 반환한다.
   *
   * <p>응답 본문은 무시하고 상태 코드와 지연 시간만 기록한다.
   * 부하 테스트에서는 응답 본문보다 성능 측정이 목적이기 때문이다.
   *
   * <p>지연 시간은 {@link System#nanoTime()}을 사용하여 나노초 단위로
   * 측정한 후 밀리초로 변환한다.
   *
   * @param request 전송할 HTTP 요청
   * @return 요청 결과 - 성공 시 {@link RequestResult.Success},
   *         실패 시 {@link RequestResult.Failure}
   */
  @Override
  public RequestResult send(HttpRequest request) {
    long startTime = System.nanoTime();

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

      long latency = toMillis(startTime);
      return new RequestResult.Success(response.statusCode(), latency);

    } catch (HttpTimeoutException e) {
      return createFailure(startTime, e.getMessage(), ErrorType.TIMEOUT);

    } catch (ConnectException e) {
      return createFailure(startTime, e.getMessage(), ErrorType.CONNECTION_REFUSED);

    } catch (SocketException e) {
      return createFailure(startTime, e.getMessage(), ErrorType.CONNECTION_RESET);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return createFailure(startTime, "Request interrupted", ErrorType.UNKNOWN);

    } catch (IOException e) {
      String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      return createFailure(startTime, message, ErrorType.UNKNOWN);

    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      return createFailure(startTime, message, ErrorType.UNKNOWN);
    }
  }

  /**
   * 실패 결과를 생성한다.
   *
   * <p>시작 시간으로부터 경과 시간을 계산하여 실패 결과에 포함한다.
   *
   * @param startTime 요청 시작 시간 (나노초)
   * @param message   에러 메시지
   * @param type      에러 타입
   * @return 실패 결과
   */
  private RequestResult.Failure createFailure(long startTime, String message, ErrorType type) {
    long latency = toMillis(startTime);
    return new RequestResult.Failure(message, type, latency);
  }

  /**
   * 나노초 시작 시간을 밀리초 경과 시간으로 변환한다.
   *
   * @param startNanos 시작 시간 (나노초)
   * @return 경과 시간 (밀리초)
   */
  private long toMillis(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }
}