package io.github.junhyeong9812.overload.scenario.scenario.domain;

import io.github.junhyeong9812.overload.core.config.HttpMethod;

import java.util.Map;

/**
 * 시나리오의 개별 Step을 정의하는 불변(Immutable) 레코드.
 *
 * <p>각 Step은 하나의 HTTP 요청과 응답에서 추출할 변수를 정의한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ScenarioStep loginStep = new ScenarioStep(
 *     "login",
 *     "로그인",
 *     HttpMethod.POST,
 *     "http://auth-service/api/login",
 *     Map.of("Content-Type", "application/json"),
 *     "{\"username\":\"test\",\"password\":\"1234\"}",
 *     Map.of("token", "$.data.accessToken"),
 *     5000
 * );
 * }</pre>
 *
 * @param id        Step 고유 식별자 (변수 참조 시 사용)
 * @param name      Step 표시 이름
 * @param method    HTTP 메서드
 * @param url       요청 URL (변수 치환 가능: ${stepId.varName})
 * @param headers   HTTP 헤더 (변수 치환 가능)
 * @param body      요청 본문 (변수 치환 가능, nullable)
 * @param extract   추출할 변수 맵 (변수명 → JSONPath 또는 추출 경로)
 * @param timeoutMs 요청 타임아웃 (밀리초)
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ScenarioStep(
    String id,
    String name,
    HttpMethod method,
    String url,
    Map<String, String> headers,
    String body,
    Map<String, String> extract,
    long timeoutMs
) {

  /**
   * ScenarioStep의 Compact Constructor.
   *
   * <p>불변 맵으로 변환하고 기본값을 설정한다.
   */
  public ScenarioStep {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Step id must not be blank");
    }
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Step url must not be blank");
    }
    if (method == null) {
      method = HttpMethod.GET;
    }
    if (name == null || name.isBlank()) {
      name = id;
    }
    if (headers == null) {
      headers = Map.of();
    } else {
      headers = Map.copyOf(headers);
    }
    if (extract == null) {
      extract = Map.of();
    } else {
      extract = Map.copyOf(extract);
    }
    if (timeoutMs <= 0) {
      timeoutMs = 30000; // 기본 30초
    }
  }

  /**
   * 추출할 변수가 있는지 확인한다.
   *
   * @return 추출할 변수가 있으면 true
   */
  public boolean hasExtraction() {
    return !extract.isEmpty();
  }
}