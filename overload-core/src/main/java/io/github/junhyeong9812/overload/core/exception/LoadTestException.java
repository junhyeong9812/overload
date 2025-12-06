package io.github.junhyeong9812.overload.core.exception;

/**
 * 부하 테스트 실행 중 발생하는 기본 예외 클래스.
 *
 * <p>이 예외는 테스트 실행 중 복구 불가능한 오류가 발생했을 때 던져진다.
 * {@link RuntimeException}을 상속하여 체크 예외 처리 부담을 줄인다.
 *
 * <p><b>발생 상황:</b>
 * <ul>
 *   <li>테스트 실행 중 인터럽트 발생</li>
 *   <li>설정 오류</li>
 *   <li>시스템 리소스 부족</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * try {
 *     TestResult result = LoadTester.run(config);
 * } catch (LoadTestException e) {
 *     System.err.println("Test failed: " + e.getMessage());
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see HttpExecutionException
 */
public class LoadTestException extends RuntimeException {

  /**
   * 지정된 메시지로 예외를 생성한다.
   *
   * @param message 예외 메시지
   */
  public LoadTestException(String message) {
    super(message);
  }

  /**
   * 지정된 메시지와 원인 예외로 예외를 생성한다.
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public LoadTestException(String message, Throwable cause) {
    super(message, cause);
  }
}