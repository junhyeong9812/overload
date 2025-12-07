package io.github.junhyeong9812.overload.starter.dto;

/**
 * 개별 HTTP 요청 로그를 나타내는 DTO.
 *
 * <p>부하 테스트 중 각 요청의 결과를 실시간으로 전달하기 위해 사용된다.
 * WebSocket을 통해 대시보드에 전송되어 실시간 요청 로그를 표시한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * // 성공한 요청
 * RequestLog log = RequestLog.success(1, 200, 45L);
 *
 * // 실패한 요청
 * RequestLog log = RequestLog.failure(2, 123L, "Connection refused");
 * }</pre>
 *
 * @param requestNumber 요청 순번 (1부터 시작)
 * @param success       요청 성공 여부
 * @param statusCode    HTTP 상태 코드 (실패 시 0)
 * @param latencyMs     응답 시간 (밀리초)
 * @param error         에러 메시지 (성공 시 null)
 * @author junhyeong9812
 * @since 1.1.0
 */
public record RequestLog(
    int requestNumber,
    boolean success,
    int statusCode,
    long latencyMs,
    String error
) {

  /**
   * 성공한 요청에 대한 RequestLog를 생성한다.
   *
   * @param requestNumber 요청 순번
   * @param statusCode    HTTP 상태 코드
   * @param latencyMs     응답 시간 (밀리초)
   * @return 성공 RequestLog 인스턴스
   */
  public static RequestLog success(int requestNumber, int statusCode, long latencyMs) {
    return new RequestLog(requestNumber, true, statusCode, latencyMs, null);
  }

  /**
   * 실패한 요청에 대한 RequestLog를 생성한다.
   *
   * @param requestNumber 요청 순번
   * @param latencyMs     실패까지 소요 시간 (밀리초)
   * @param error         에러 메시지
   * @return 실패 RequestLog 인스턴스
   */
  public static RequestLog failure(int requestNumber, long latencyMs, String error) {
    return new RequestLog(requestNumber, false, 0, latencyMs, error);
  }
}