package io.github.junhyeong9812.overload.scenario.builder;

import com.jayway.jsonpath.JsonPath;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.scenario.scenario.domain.FailureStrategy;
import io.github.junhyeong9812.overload.scenario.scenario.domain.Scenario;
import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 문자열을 Scenario로 변환하는 파서.
 *
 * <p><b>JSON 형식:</b>
 * <pre>{@code
 * {
 *   "name": "시나리오 이름",
 *   "failureStrategy": "STOP",
 *   "retryCount": 0,
 *   "retryDelayMs": 1000,
 *   "steps": [
 *     {
 *       "id": "login",
 *       "name": "로그인",
 *       "method": "POST",
 *       "url": "http://api/login",
 *       "headers": {"Content-Type": "application/json"},
 *       "body": "{\"username\":\"test\"}",
 *       "extract": {"token": "$.data.accessToken"},
 *       "timeoutMs": 5000
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ScenarioParser {

  private ScenarioParser() {
    // Static utility class
  }

  /**
   * JSON 문자열을 Scenario로 변환한다.
   *
   * @param json JSON 문자열
   * @return 파싱된 Scenario
   * @throws IllegalArgumentException JSON 형식이 잘못된 경우
   */
  @SuppressWarnings("unchecked")
  public static Scenario parse(String json) {
    try {
      Object document = JsonPath.parse(json).json();

      if (!(document instanceof Map)) {
        throw new IllegalArgumentException("JSON root must be an object");
      }

      Map<String, Object> root = (Map<String, Object>) document;

      String name = getString(root, "name", "Unnamed Scenario");
      FailureStrategy strategy = parseFailureStrategy(getString(root, "failureStrategy", "STOP"));
      int retryCount = getInt(root, "retryCount", 0);
      long retryDelayMs = getLong(root, "retryDelayMs", 1000);

      List<Map<String, Object>> stepsJson = (List<Map<String, Object>>) root.get("steps");
      if (stepsJson == null || stepsJson.isEmpty()) {
        throw new IllegalArgumentException("Scenario must have at least one step");
      }

      List<ScenarioStep> steps = new ArrayList<>();
      for (Map<String, Object> stepJson : stepsJson) {
        steps.add(parseStep(stepJson));
      }

      return new Scenario(name, steps, strategy, retryCount, retryDelayMs);

    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse scenario JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Step JSON을 ScenarioStep으로 변환한다.
   */
  @SuppressWarnings("unchecked")
  private static ScenarioStep parseStep(Map<String, Object> stepJson) {
    String id = getString(stepJson, "id", null);
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Step must have an id");
    }

    String name = getString(stepJson, "name", id);
    HttpMethod method = parseHttpMethod(getString(stepJson, "method", "GET"));
    String url = getString(stepJson, "url", null);

    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Step must have a url: " + id);
    }

    Map<String, String> headers = new HashMap<>();
    Object headersObj = stepJson.get("headers");
    if (headersObj instanceof Map) {
      ((Map<String, Object>) headersObj).forEach((k, v) -> headers.put(k, String.valueOf(v)));
    }

    String body = getString(stepJson, "body", null);

    Map<String, String> extract = new HashMap<>();
    Object extractObj = stepJson.get("extract");
    if (extractObj instanceof Map) {
      ((Map<String, Object>) extractObj).forEach((k, v) -> extract.put(k, String.valueOf(v)));
    }

    long timeoutMs = getLong(stepJson, "timeoutMs", 30000);

    return new ScenarioStep(id, name, method, url, headers, body, extract, timeoutMs);
  }

  /**
   * 문자열을 FailureStrategy로 변환한다.
   */
  private static FailureStrategy parseFailureStrategy(String value) {
    try {
      return FailureStrategy.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return FailureStrategy.STOP;
    }
  }

  /**
   * 문자열을 HttpMethod로 변환한다.
   */
  private static HttpMethod parseHttpMethod(String value) {
    try {
      return HttpMethod.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return HttpMethod.GET;
    }
  }

  /**
   * Map에서 String 값을 가져온다.
   */
  private static String getString(Map<String, Object> map, String key, String defaultValue) {
    Object value = map.get(key);
    return value != null ? String.valueOf(value) : defaultValue;
  }

  /**
   * Map에서 int 값을 가져온다.
   */
  private static int getInt(Map<String, Object> map, String key, int defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return defaultValue;
  }

  /**
   * Map에서 long 값을 가져온다.
   */
  private static long getLong(Map<String, Object> map, String key, long defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return defaultValue;
  }
}