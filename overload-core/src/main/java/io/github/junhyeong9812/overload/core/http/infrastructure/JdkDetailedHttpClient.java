package io.github.junhyeong9812.overload.core.http.infrastructure;

import io.github.junhyeong9812.overload.core.http.application.port.DetailedHttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;
import io.github.junhyeong9812.overload.core.http.domain.ErrorType;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;

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
 * JDK HttpClient 기반의 상세 응답 HTTP 클라이언트 구현체.
 *
 * <p>시나리오 테스트용으로 응답 본문과 헤더를 모두 반환한다.
 * 부하 테스트용 {@link JdkHttpClient}와 달리 응답을 메모리에 유지한다.
 *
 * <p><b>특징:</b>
 * <ul>
 *   <li>응답 본문 및 헤더 전체 반환</li>
 *   <li>Virtual Thread 친화적 Executor 사용</li>
 *   <li>나노초 단위 정밀 지연 시간 측정</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * DetailedHttpClientPort client = new JdkDetailedHttpClient(Duration.ofSeconds(5));
 *
 * HttpRequest request = HttpRequest.from(
 *     "https://api.example.com/login",
 *     HttpMethod.POST,
 *     Map.of("Content-Type", "application/json"),
 *     "{\"username\":\"test\"}"
 * );
 *
 * DetailedRequestResult result = client.send(request);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see DetailedHttpClientPort
 */
public class JdkDetailedHttpClient implements DetailedHttpClientPort {

  private final HttpClient client;
  private final Duration timeout;

  /**
   * 지정된 타임아웃으로 JdkDetailedHttpClient를 생성한다.
   *
   * <p>Virtual Thread Executor를 사용하여 높은 동시성을 지원한다.
   *
   * @param timeout 연결 및 요청 타임아웃
   */
  public JdkDetailedHttpClient(Duration timeout) {
    this.timeout = timeout;
    this.client = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();
  }

  /**
   * HTTP 요청을 전송하고 상세 결과를 반환한다.
   *
   * <p>응답 본문을 문자열로 읽어 반환하며, 모든 응답 헤더를 포함한다.
   *
   * @param request 전송할 HTTP 요청
   * @return 상세 요청 결과
   */
  @Override
  public DetailedRequestResult send(HttpRequest request) {
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

      // 응답 본문을 문자열로 읽음
      HttpResponse<String> response = client.send(
          builder.build(),
          HttpResponse.BodyHandlers.ofString()
      );

      long latency = toMillis(startTime);
      return new DetailedRequestResult.Success(
          response.statusCode(),
          latency,
          response.body(),
          response.headers().map()
      );

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
   * @param startTime 요청 시작 시간 (나노초)
   * @param message   에러 메시지
   * @param type      에러 타입
   * @return 실패 결과
   */
  private DetailedRequestResult.Failure createFailure(long startTime, String message, ErrorType type) {
    long latency = toMillis(startTime);
    return new DetailedRequestResult.Failure(message, type, latency);
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