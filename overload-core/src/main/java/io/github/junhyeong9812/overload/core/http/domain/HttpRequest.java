package io.github.junhyeong9812.overload.core.http.domain;

import io.github.junhyeong9812.overload.core.config.HttpMethod;

import java.util.Map;

/**
 * HTTP 요청 도메인 모델
 *
 * @param url     요청 URL
 * @param method  HTTP 메서드
 * @param headers HTTP 헤더
 * @param body    요청 본문
 */
public record HttpRequest(
    String url,
    HttpMethod method,
    Map<String, String> headers,
    String body
) {

  public HttpRequest {
    headers = headers != null ? Map.copyOf(headers) : Map.of();
  }

  /**
   * 팩토리 메서드
   */
  public static HttpRequest of(String url, HttpMethod method, Map<String, String> headers, String body) {
    return new HttpRequest(url, method, headers, body);
  }

  /**
   * GET 요청 생성
   */
  public static HttpRequest get(String url) {
    return new HttpRequest(url, HttpMethod.GET, Map.of(), null);
  }

  /**
   * POST 요청 생성
   */
  public static HttpRequest post(String url, String body) {
    return new HttpRequest(url, HttpMethod.POST, Map.of(), body);
  }

  /**
   * 헤더가 있는지 확인
   */
  public boolean hasBody() {
    return body != null && !body.isEmpty();
  }
}