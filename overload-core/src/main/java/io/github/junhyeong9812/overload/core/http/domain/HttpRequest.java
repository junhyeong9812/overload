package io.github.junhyeong9812.overload.core.http.domain;

import io.github.junhyeong9812.overload.core.config.HttpMethod;

import java.util.Map;

/**
 * HTTP 요청을 표현하는 불변(Immutable) 도메인 모델.
 *
 * <p>이 클래스는 HTTP 요청의 모든 정보를 캡슐화한다.
 * {@link io.github.junhyeong9812.overload.core.config.LoadTestConfig}에서
 * 변환되어 HTTP 클라이언트에 전달된다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * HttpRequest request = HttpRequest.from(
 *     "https://api.example.com/users",
 *     HttpMethod.GET,
 *     Map.of("Accept", "application/json"),
 *     null
 * );
 * }</pre>
 *
 * @param url     요청 대상 URL
 * @param method  HTTP 메서드
 * @param headers HTTP 헤더 맵 (불변)
 * @param body    요청 본문 (nullable)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record HttpRequest(
    String url,
    HttpMethod method,
    Map<String, String> headers,
    String body
) {

  /**
   * HttpRequest 인스턴스를 생성하는 정적 팩토리 메서드.
   *
   * <p>headers 맵은 불변으로 복사된다.
   *
   * @param url     요청 대상 URL
   * @param method  HTTP 메서드
   * @param headers HTTP 헤더 맵
   * @param body    요청 본문 (nullable)
   * @return 새로운 HttpRequest 인스턴스
   */
  public static HttpRequest from(
      String url,
      HttpMethod method,
      Map<String, String> headers,
      String body) {
    return new HttpRequest(url, method, Map.copyOf(headers), body);
  }
}