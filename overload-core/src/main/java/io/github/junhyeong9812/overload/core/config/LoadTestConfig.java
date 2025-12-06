package io.github.junhyeong9812.overload.core.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 부하 테스트 설정
 *
 * @param url           대상 URL
 * @param method        HTTP 메서드
 * @param headers       HTTP 헤더
 * @param body          요청 본문
 * @param concurrency   동시 요청 수
 * @param totalRequests 총 요청 수
 * @param timeout       요청 타임아웃
 */
public record LoadTestConfig(
    String url,
    HttpMethod method,
    Map<String, String> headers,
    String body,
    int concurrency,
    int totalRequests,
    Duration timeout
) {

  public LoadTestConfig {
    Objects.requireNonNull(url, "URL is required");
    if (url.isBlank()) {
      throw new IllegalArgumentException("URL cannot be blank");
    }
    if (concurrency < 1) {
      throw new IllegalArgumentException("Concurrency must be >= 1");
    }
    if (totalRequests < 1) {
      throw new IllegalArgumentException("Total requests must be >= 1");
    }
    Objects.requireNonNull(timeout, "Timeout is required");

    // Immutable copy
    headers = headers != null ? Map.copyOf(headers) : Map.of();
    method = method != null ? method : HttpMethod.GET;
  }

  /**
   * Builder 생성
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * LoadTestConfig Builder
   */
  public static class Builder {
    private String url;
    private HttpMethod method = HttpMethod.GET;
    private final Map<String, String> headers = new HashMap<>();
    private String body;
    private int concurrency = 10;
    private int totalRequests = 100;
    private Duration timeout = Duration.ofSeconds(5);

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder method(HttpMethod method) {
      this.method = method;
      return this;
    }

    public Builder header(String key, String value) {
      this.headers.put(key, value);
      return this;
    }

    public Builder headers(Map<String, String> headers) {
      if (headers != null) {
        this.headers.putAll(headers);
      }
      return this;
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder concurrency(int concurrency) {
      this.concurrency = concurrency;
      return this;
    }

    public Builder totalRequests(int totalRequests) {
      this.totalRequests = totalRequests;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public LoadTestConfig build() {
      return new LoadTestConfig(
          url,
          method,
          headers,
          body,
          concurrency,
          totalRequests,
          timeout
      );
    }
  }
}