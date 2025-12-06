package io.github.junhyeong9812.overload.core.http.application.port;

import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

/**
 * HTTP 클라이언트를 위한 출력 포트(Output Port) 인터페이스.
 *
 * <p>이 인터페이스는 헥사고날 아키텍처(Hexagonal Architecture)의 포트 역할을 한다.
 * 코어 로직은 이 인터페이스에만 의존하며, 실제 HTTP 클라이언트 구현체는
 * 인프라스트럭처 레이어에서 어댑터로 제공된다.
 *
 * <p><b>구현체:</b>
 * <ul>
 *   <li>{@code JdkHttpClient} - JDK 11+ HttpClient 기반 (기본)</li>
 *   <li>{@code OkHttpClient} - OkHttp 기반 (확장)</li>
 *   <li>{@code ApacheHttpClient} - Apache HttpClient 기반 (확장)</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * // 실제 구현체 사용
 * HttpClientPort client = new JdkHttpClient(Duration.ofSeconds(5));
 * RequestResult result = client.send(request);
 *
 * // 테스트용 Mock
 * HttpClientPort mockClient = request ->
 *     new RequestResult.Success(200, 100);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see HttpRequest
 * @see RequestResult
 */
public interface HttpClientPort {

  /**
   * HTTP 요청을 전송하고 결과를 반환한다.
   *
   * <p>이 메서드는 동기적으로 실행되며, 요청이 완료될 때까지 블로킹된다.
   * Virtual Thread 환경에서 사용하면 효율적인 I/O 처리가 가능하다.
   *
   * <p><b>결과 타입:</b>
   * <ul>
   *   <li>{@link RequestResult.Success} - HTTP 응답 수신 성공</li>
   *   <li>{@link RequestResult.Failure} - 네트워크 오류, 타임아웃 등으로 실패</li>
   * </ul>
   *
   * @param request 전송할 HTTP 요청
   * @return 요청 결과 - 성공 또는 실패
   */
  RequestResult send(HttpRequest request);
}