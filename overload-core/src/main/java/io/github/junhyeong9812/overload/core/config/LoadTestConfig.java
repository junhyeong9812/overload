package io.github.junhyeong9812.overload.core.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 부하 테스트 설정을 정의하는 불변(Immutable) 레코드.
 *
 * <p>Builder 패턴을 통해 유연하게 설정을 구성할 수 있으며,
 * 생성 시 유효성 검증을 수행한다.
 *
 * <p><b>기본값:</b>
 * <ul>
 *   <li>{@code method} - {@link HttpMethod#GET}</li>
 *   <li>{@code concurrency} - 10</li>
 *   <li>{@code totalRequests} - 100</li>
 *   <li>{@code timeout} - 5초</li>
 *   <li>{@code headers} - 빈 맵</li>
 *   <li>{@code body} - null</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * LoadTestConfig config = LoadTestConfig.builder()
 *     .url("https://api.example.com/users")
 *     .method(HttpMethod.POST)
 *     .header("Content-Type", "application/json")
 *     .body("{\"name\": \"test\"}")
 *     .concurrency(100)
 *     .totalRequests(10000)
 *     .timeout(Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 *
 * @param url           대상 URL (필수)
 * @param method        HTTP 메서드 (기본값: GET)
 * @param headers       HTTP 헤더 맵 (불변)
 * @param body          요청 본문 (nullable)
 * @param concurrency   동시 요청 수 (기본값: 10)
 * @param totalRequests 총 요청 수 (기본값: 100)
 * @param timeout       요청 타임아웃 (기본값: 5초)
 *
 * @author junhyeong9812
 * @since 1.0.0
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

  /**
   * Compact constructor - 유효성 검증 수행.
   *
   * @throws NullPointerException     url이 null인 경우
   * @throws IllegalArgumentException concurrency 또는 totalRequests가 1 미만인 경우
   */
  public LoadTestConfig {
    Objects.requireNonNull(url, "URL is required");
    if (concurrency < 1) {
      throw new IllegalArgumentException("Concurrency must be >= 1");
    }
    if (totalRequests < 1) {
      throw new IllegalArgumentException("Total requests must be >= 1");
    }
  }

  /**
   * 새로운 Builder 인스턴스를 생성한다.
   *
   * @return 새로운 {@link Builder} 인스턴스
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@link LoadTestConfig} 인스턴스를 생성하기 위한 빌더 클래스.
   *
   * @author junhyeong9812
   * @since 1.0.0
   */
  public static class Builder {

    private String url;
    private HttpMethod method = HttpMethod.GET;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    private int concurrency = 10;
    private int totalRequests = 100;
    private Duration timeout = Duration.ofSeconds(5);

    /**
     * 대상 URL을 설정한다.
     *
     * @param url 테스트 대상 URL (필수)
     * @return this
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * HTTP 메서드를 설정한다.
     *
     * @param method HTTP 메서드 (기본값: GET)
     * @return this
     */
    public Builder method(HttpMethod method) {
      this.method = method;
      return this;
    }

    /**
     * 단일 HTTP 헤더를 추가한다.
     *
     * @param key   헤더 이름
     * @param value 헤더 값
     * @return this
     */
    public Builder header(String key, String value) {
      this.headers.put(key, value);
      return this;
    }

    /**
     * 여러 HTTP 헤더를 한 번에 추가한다.
     *
     * @param headers 추가할 헤더 맵
     * @return this
     */
    public Builder headers(Map<String, String> headers) {
      this.headers.putAll(headers);
      return this;
    }

    /**
     * 요청 본문을 설정한다.
     *
     * @param body 요청 본문
     * @return this
     */
    public Builder body(String body) {
      this.body = body;
      return this;
    }

    /**
     * 동시 요청 수를 설정한다.
     *
     * @param concurrency 동시에 실행할 요청 수 (기본값: 10)
     * @return this
     */
    public Builder concurrency(int concurrency) {
      this.concurrency = concurrency;
      return this;
    }

    /**
     * 총 요청 수를 설정한다.
     *
     * @param totalRequests 총 요청 수 (기본값: 100)
     * @return this
     */
    public Builder totalRequests(int totalRequests) {
      this.totalRequests = totalRequests;
      return this;
    }

    /**
     * 요청 타임아웃을 설정한다.
     *
     * @param timeout 요청 타임아웃 (기본값: 5초)
     * @return this
     */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * 설정된 값으로 {@link LoadTestConfig} 인스턴스를 생성한다.
     *
     * @return 새로운 LoadTestConfig 인스턴스
     * @throws NullPointerException     url이 null인 경우
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    public LoadTestConfig build() {
      return new LoadTestConfig(
          url, method, Map.copyOf(headers), body,
          concurrency, totalRequests, timeout
      );
    }
  }
}