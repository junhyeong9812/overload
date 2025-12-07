package io.github.junhyeong9812.overload.core.http.domain;

import java.util.List;
import java.util.Map;

/**
 * 상세 HTTP 응답 결과를 표현하는 sealed 인터페이스.
 *
 * <p>시나리오 테스트용으로 응답 본문과 헤더를 포함한다.
 * 이전 요청의 응답에서 값을 추출하여 다음 요청에 사용할 때 필요하다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * DetailedRequestResult result = detailedClient.send(request);
 *
 * if (result instanceof DetailedRequestResult.Success success) {
 *     String token = JsonPath.read(success.responseBody(), "$.data.token");
 *     String cookie = success.getHeader("Set-Cookie");
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see RequestResult
 */
public sealed interface DetailedRequestResult
    permits DetailedRequestResult.Success, DetailedRequestResult.Failure {

  /**
   * 요청 지연 시간을 반환한다.
   *
   * @return 지연 시간 (밀리초)
   */
  long latencyMs();

  /**
   * 성공한 요청의 상세 결과.
   *
   * @param statusCode      HTTP 상태 코드
   * @param latencyMs       지연 시간 (밀리초)
   * @param responseBody    응답 본문
   * @param responseHeaders 응답 헤더 (키 → 값 목록)
   */
  record Success(
      int statusCode,
      long latencyMs,
      String responseBody,
      Map<String, List<String>> responseHeaders
  ) implements DetailedRequestResult {

    /**
     * HTTP 성공 응답인지 확인한다.
     *
     * @return 2xx 응답이면 true
     */
    public boolean isHttpSuccess() {
      return statusCode >= 200 && statusCode < 300;
    }

    /**
     * 지정된 이름의 첫 번째 헤더 값을 반환한다.
     *
     * @param name 헤더 이름 (대소문자 구분)
     * @return 헤더 값, 없으면 null
     */
    public String getHeader(String name) {
      List<String> values = responseHeaders.get(name);
      return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    /**
     * 지정된 이름의 모든 헤더 값을 반환한다.
     *
     * @param name 헤더 이름 (대소문자 구분)
     * @return 헤더 값 목록, 없으면 빈 리스트
     */
    public List<String> getHeaders(String name) {
      List<String> values = responseHeaders.get(name);
      return values != null ? values : List.of();
    }
  }

  /**
   * 실패한 요청의 상세 결과.
   *
   * @param errorMessage 에러 메시지
   * @param errorType    에러 유형
   * @param latencyMs    지연 시간 (밀리초)
   */
  record Failure(
      String errorMessage,
      ErrorType errorType,
      long latencyMs
  ) implements DetailedRequestResult {
  }
}