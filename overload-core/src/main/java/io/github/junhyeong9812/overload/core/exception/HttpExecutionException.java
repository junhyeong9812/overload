package io.github.junhyeong9812.overload.core.exception;

/**
 * HTTP 요청 실행 중 발생하는 예외 클래스.
 *
 * <p>{@link LoadTestException}을 상속하며, HTTP 통신 관련 오류를 나타낸다.
 *
 * <p><b>발생 상황:</b>
 * <ul>
 *   <li>연결 타임아웃</li>
 *   <li>연결 거부 (Connection Refused)</li>
 *   <li>연결 리셋 (Connection Reset)</li>
 *   <li>잘못된 URL</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * try {
 *     RequestResult result = httpClient.send(request);
 * } catch (HttpExecutionException e) {
 *     System.err.println("HTTP error: " + e.getMessage());
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see LoadTestException
 */
public class HttpExecutionException extends LoadTestException {

  /**
   * 지정된 메시지로 예외를 생성한다.
   *
   * @param message 예외 메시지
   */
  public HttpExecutionException(String message) {
    super(message);
  }

  /**
   * 지정된 메시지와 원인 예외로 예외를 생성한다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public HttpExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}