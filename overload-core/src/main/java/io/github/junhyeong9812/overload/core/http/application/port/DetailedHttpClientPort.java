package io.github.junhyeong9812.overload.core.http.application.port;

import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;

/**
 * 상세 응답을 반환하는 HTTP 클라이언트 포트.
 *
 * <p>시나리오 테스트에서 응답 본문과 헤더를 추출해야 할 때 사용한다.
 * 부하 테스트용 {@link HttpClientPort}와 달리 응답 전체를 메모리에 유지한다.
 *
 * <p><b>사용 시나리오:</b>
 * <ul>
 *   <li>로그인 응답에서 토큰 추출</li>
 *   <li>이전 응답의 ID를 다음 요청에 사용</li>
 *   <li>Set-Cookie 헤더 값 추출</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * DetailedHttpClientPort client = new JdkDetailedHttpClient(Duration.ofSeconds(5));
 * DetailedRequestResult result = client.send(request);
 *
 * if (result instanceof DetailedRequestResult.Success success) {
 *     String body = success.responseBody();
 *     String token = JsonPath.read(body, "$.data.accessToken");
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see HttpClientPort
 * @see DetailedRequestResult
 */
public interface DetailedHttpClientPort {

  /**
   * HTTP 요청을 전송하고 상세 결과를 반환한다.
   *
   * <p>응답 본문과 헤더를 모두 포함하여 반환한다.
   * 대용량 응답의 경우 메모리 사용에 주의해야 한다.
   *
   * @param request 전송할 HTTP 요청
   * @return 상세 요청 결과 - 성공 시 {@link DetailedRequestResult.Success},
   *         실패 시 {@link DetailedRequestResult.Failure}
   */
  DetailedRequestResult send(HttpRequest request);
}