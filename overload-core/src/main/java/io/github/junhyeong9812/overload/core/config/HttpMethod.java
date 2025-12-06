package io.github.junhyeong9812.overload.core.config;

/**
 * HTTP 요청 메서드를 정의하는 열거형.
 *
 * <p>부하 테스트에서 사용할 HTTP 메서드를 나타낸다.
 * RFC 7231에 정의된 표준 메서드를 지원한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * LoadTestConfig config = LoadTestConfig.builder()
 *     .url("https://api.example.com/users")
 *     .method(HttpMethod.POST)
 *     .body("{\"name\": \"test\"}")
 *     .build();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public enum HttpMethod {

  /** GET 메서드 - 리소스 조회 */
  GET,

  /** POST 메서드 - 리소스 생성 */
  POST,

  /** PUT 메서드 - 리소스 전체 수정 */
  PUT,

  /** DELETE 메서드 - 리소스 삭제 */
  DELETE,

  /** PATCH 메서드 - 리소스 부분 수정 */
  PATCH,

  /** HEAD 메서드 - 헤더만 조회 */
  HEAD,

  /** OPTIONS 메서드 - 지원 메서드 조회 */
  OPTIONS
}