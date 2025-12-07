package io.github.junhyeong9812.overload.scenario.variable.application;

import io.github.junhyeong9812.overload.scenario.variable.domain.VariableContext;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 변수 치환을 수행하는 서비스.
 *
 * <p>${stepId.varName} 형식의 변수를 실제 값으로 치환한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * VariableContext context = new VariableContext();
 * context.put("login", "token", "abc123");
 *
 * VariableResolver resolver = new VariableResolver();
 * String result = resolver.resolve("Bearer ${login.token}", context);
 * // result: "Bearer abc123"
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class VariableResolver {

  private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

  /**
   * 문자열에서 변수를 치환한다.
   *
   * @param template 변수가 포함된 템플릿 문자열
   * @param context  변수 컨텍스트
   * @return 변수가 치환된 문자열
   */
  public String resolve(String template, VariableContext context) {
    if (template == null || template.isBlank()) {
      return template;
    }

    Matcher matcher = VAR_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String varRef = matcher.group(1);  // stepId.varName
      String replacement = resolveVariable(varRef, context);
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * 헤더 맵의 모든 값에서 변수를 치환한다.
   *
   * @param headers 헤더 맵
   * @param context 변수 컨텍스트
   * @return 변수가 치환된 헤더 맵
   */
  public Map<String, String> resolveHeaders(Map<String, String> headers, VariableContext context) {
    if (headers == null || headers.isEmpty()) {
      return headers;
    }

    Map<String, String> resolved = new HashMap<>();
    headers.forEach((key, value) -> resolved.put(key, resolve(value, context)));
    return resolved;
  }

  /**
   * 단일 변수 참조를 해석한다.
   *
   * @param varRef  변수 참조 (stepId.varName)
   * @param context 변수 컨텍스트
   * @return 해석된 값 (없으면 원본 ${...} 유지)
   */
  private String resolveVariable(String varRef, VariableContext context) {
    int dotIndex = varRef.indexOf('.');
    if (dotIndex == -1) {
      return "${" + varRef + "}";  // 잘못된 형식
    }

    String stepId = varRef.substring(0, dotIndex);
    String varName = varRef.substring(dotIndex + 1);

    Object value = context.get(stepId, varName);
    if (value == null) {
      return "${" + varRef + "}";  // 값 없으면 원본 유지
    }

    return String.valueOf(value);
  }
}