package io.github.junhyeong9812.overload.scenario.builder;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioStep;

import java.util.HashMap;
import java.util.Map;

/**
 * ScenarioStep을 생성하는 빌더.
 *
 * <p>Fluent API로 Step을 정의할 수 있다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ScenarioStep step = new StepBuilder("login")
 *     .name("로그인")
 *     .post("http://auth/api/login")
 *     .header("Content-Type", "application/json")
 *     .body("{\"username\":\"test\"}")
 *     .extract("token", "$.data.accessToken")
 *     .timeout(5000)
 *     .build();
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class StepBuilder {

  private final String id;
  private String name;
  private HttpMethod method = HttpMethod.GET;
  private String url;
  private final Map<String, String> headers = new HashMap<>();
  private String body;
  private final Map<String, String> extract = new HashMap<>();
  private long timeoutMs = 30000;

  /**
   * 지정된 ID로 StepBuilder를 생성한다.
   *
   * @param id Step 고유 식별자
   */
  public StepBuilder(String id) {
    this.id = id;
    this.name = id;
  }

  /**
   * Step 표시 이름을 설정한다.
   *
   * @param name 표시 이름
   * @return this
   */
  public StepBuilder name(String name) {
    this.name = name;
    return this;
  }

  /**
   * HTTP GET 요청을 설정한다.
   *
   * @param url 요청 URL
   * @return this
   */
  public StepBuilder get(String url) {
    this.method = HttpMethod.GET;
    this.url = url;
    return this;
  }

  /**
   * HTTP POST 요청을 설정한다.
   *
   * @param url 요청 URL
   * @return this
   */
  public StepBuilder post(String url) {
    this.method = HttpMethod.POST;
    this.url = url;
    return this;
  }

  /**
   * HTTP PUT 요청을 설정한다.
   *
   * @param url 요청 URL
   * @return this
   */
  public StepBuilder put(String url) {
    this.method = HttpMethod.PUT;
    this.url = url;
    return this;
  }

  /**
   * HTTP DELETE 요청을 설정한다.
   *
   * @param url 요청 URL
   * @return this
   */
  public StepBuilder delete(String url) {
    this.method = HttpMethod.DELETE;
    this.url = url;
    return this;
  }

  /**
   * HTTP PATCH 요청을 설정한다.
   *
   * @param url 요청 URL
   * @return this
   */
  public StepBuilder patch(String url) {
    this.method = HttpMethod.PATCH;
    this.url = url;
    return this;
  }

  /**
   * HTTP 메서드와 URL을 직접 설정한다.
   *
   * @param method HTTP 메서드
   * @param url    요청 URL
   * @return this
   */
  public StepBuilder method(HttpMethod method, String url) {
    this.method = method;
    this.url = url;
    return this;
  }

  /**
   * HTTP 헤더를 추가한다.
   *
   * @param name  헤더 이름
   * @param value 헤더 값 (변수 치환 가능: ${stepId.varName})
   * @return this
   */
  public StepBuilder header(String name, String value) {
    this.headers.put(name, value);
    return this;
  }

  /**
   * 여러 HTTP 헤더를 추가한다.
   *
   * @param headers 헤더 맵
   * @return this
   */
  public StepBuilder headers(Map<String, String> headers) {
    this.headers.putAll(headers);
    return this;
  }

  /**
   * 요청 본문을 설정한다.
   *
   * @param body 요청 본문 (변수 치환 가능: ${stepId.varName})
   * @return this
   */
  public StepBuilder body(String body) {
    this.body = body;
    return this;
  }

  /**
   * 응답에서 값을 추출하여 변수에 저장한다.
   *
   * <p>추출 경로 형식:
   * <ul>
   *   <li>JSONPath: $.data.token</li>
   *   <li>헤더: $header.Set-Cookie</li>
   *   <li>정규식: $regex.body.(pattern)</li>
   * </ul>
   *
   * @param variableName 저장할 변수 이름
   * @param path         추출 경로
   * @return this
   */
  public StepBuilder extract(String variableName, String path) {
    this.extract.put(variableName, path);
    return this;
  }

  /**
   * 요청 타임아웃을 설정한다.
   *
   * @param timeoutMs 타임아웃 (밀리초)
   * @return this
   */
  public StepBuilder timeout(long timeoutMs) {
    this.timeoutMs = timeoutMs;
    return this;
  }

  /**
   * ScenarioStep을 생성한다.
   *
   * @return 생성된 ScenarioStep
   * @throws IllegalArgumentException URL이 설정되지 않은 경우
   */
  public ScenarioStep build() {
    return new ScenarioStep(
        id,
        name,
        method,
        url,
        headers,
        body,
        extract,
        timeoutMs
    );
  }
}