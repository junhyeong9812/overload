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
   * 요청 응답 시간(지연 시간)을 밀리초 단위로 반환한다.
   *
   * @return 요청 처리에 소요된 시간 (밀리초)
   */
  long latencyMs();

  /**
   * 성공한 HTTP 요청 결과를 표현하는 레코드.
   *
   * <p>HTTP 응답이 정상적으로 수신된 경우를 나타낸다.
   * HTTP 상태 코드가 4xx, 5xx인 경우도 응답을 받았다면 Success로 처리된다.
   *
   * @param statusCode HTTP 응답 상태 코드 (예: 200, 404, 500)
   * @param latencyMs  응답 시간 (밀리초)
   */
  record Success(
      int statusCode,
      long latencyMs
  ) implements RequestResult {

    /**
     * HTTP 상태 코드가 2xx 범위(성공)인지 확인한다.
     *
     * @return 상태 코드가 200-299 범위이면 {@code true}
     */
    public boolean isHttpSuccess() {
      return statusCode >= 200 && statusCode < 300;
    }
  }

  /**
   * 실패한 HTTP 요청 결과를 표현하는 레코드.
   *
   * <p>네트워크 오류, 타임아웃, 연결 거부 등 HTTP 응답을 받지 못한 경우를 나타낸다.
   *
   * @param errorMessage 상세 오류 메시지
   * @param errorType    오류 유형 분류
   * @param latencyMs    실패까지 소요된 시간 (밀리초)
   */
  record Failure(
      String errorMessage,
      ErrorType errorType,
      long latencyMs
  ) implements RequestResult {
  }

  /**
   * HTTP 요청 실패 유형을 분류하는 열거형.
   */
  enum ErrorType {

    /** 요청 타임아웃 */
    TIMEOUT,

    /** 연결 거부 */
    CONNECTION_REFUSED,

    /** 연결 리셋 */
    CONNECTION_RESET,

    /** 알 수 없는 오류 */
    UNKNOWN
  }
}