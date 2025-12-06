package io.github.junhyeong9812.overload.core.http.domain;

import java.util.Map;

/**
 * HTTP 응답을 표현하는 불변(Immutable) 도메인 모델.
 *
 * <p>HTTP 응답의 상태 코드, 헤더, 본문을 캡슐화한다.
 * 상태 코드 범위에 따른 편의 메서드를 제공한다.
 *
 * <p><b>상태 코드 분류:</b>
 * <ul>
 *   <li>2xx - 성공 ({@link #isSuccess()})</li>
 *   <li>4xx - 클라이언트 오류 ({@link #isClientError()})</li>
 *   <li>5xx - 서버 오류 ({@link #isServerError()})</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * HttpResponse response = new HttpResponse(200, Map.of(), "OK");
 *
 * if (response.isSuccess()) {
 *     System.out.println("Success: " + response.body());
 * }
 * }</pre>
 *
 * @param statusCode HTTP 상태 코드
 * @param headers    HTTP 응답 헤더 맵
 * @param body       응답 본문 (nullable)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record HttpResponse(
    int statusCode,
    Map<String, String> headers,
    String body
) {

  /**
   * 응답이 성공(2xx)인지 확인한다.
   *
   * @return 상태 코드가 200-299 범위이면 {@code true}
   */
  public boolean isSuccess() {
    return statusCode >= 200 && statusCode < 300;
  }

  /**
   * 응답이 클라이언트 오류(4xx)인지 확인한다.
   *
   * @return 상태 코드가 400-499 범위이면 {@code true}
   */
  public boolean isClientError() {
    return statusCode >= 400 && statusCode < 500;
  }

  /**
   * 응답이 서버 오류(5xx)인지 확인한다.
   *
   * @return 상태 코드가 500 이상이면 {@code true}
   */
  public boolean isServerError() {
    return statusCode >= 500;
  }
}