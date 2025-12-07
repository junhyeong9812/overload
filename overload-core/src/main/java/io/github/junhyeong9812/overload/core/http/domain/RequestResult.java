package io.github.junhyeong9812.overload.core.http.domain;

/**
 * HTTP 요청 결과를 표현하는 Sealed Interface.
 *
 * <p>Java 17의 Sealed Classes 기능을 활용하여 요청 결과를
 * {@link Success}와 {@link Failure} 두 가지 타입으로 제한한다.
 * Pattern Matching을 통해 타입 안전한 결과 처리가 가능하다.
 *
 * <p><b>Pattern Matching 사용 예시:</b>
 * <pre>{@code
 * RequestResult result = httpClient.send(request);
 *
 * switch (result) {
 *     case RequestResult.Success s -> {
 *         System.out.println("Status: " + s.statusCode());
 *         System.out.println("Latency: " + s.latencyMs() + "ms");
 *     }
 *     case RequestResult.Failure f -> {
 *         System.err.println("Error: " + f.errorMessage());
 *     }
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public sealed interface RequestResult
    permits RequestResult.Success, RequestResult.Failure {

  /**
   * 요청 지연 시간을 반환한다.
   *
   * @return 지연 시간 (밀리초)
   */
  long latencyMs();

  /**
   * 성공한 요청 결과.
   *
   * @param statusCode HTTP 상태 코드
   * @param latencyMs  지연 시간 (밀리초)
   */
  record Success(int statusCode, long latencyMs) implements RequestResult {

    /**
     * HTTP 성공 응답인지 확인한다.
     *
     * @return 2xx 응답이면 true
     */
    public boolean isHttpSuccess() {
      return statusCode >= 200 && statusCode < 300;
    }
  }

  /**
   * 실패한 요청 결과.
   *
   * @param errorMessage 에러 메시지
   * @param errorType    에러 유형
   * @param latencyMs    지연 시간 (밀리초)
   */
  record Failure(String errorMessage, ErrorType errorType, long latencyMs) implements RequestResult {
  }
}