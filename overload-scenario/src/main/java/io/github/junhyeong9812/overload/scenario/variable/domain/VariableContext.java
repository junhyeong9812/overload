package io.github.junhyeong9812.overload.scenario.variable.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 시나리오 실행 중 추출된 변수를 저장하는 컨텍스트.
 *
 * <p>각 Step에서 추출한 값을 저장하고, 다음 Step에서 참조할 수 있다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * VariableContext context = new VariableContext();
 *
 * // 값 저장
 * context.put("login", "token", "abc123");
 * context.put("login", "userId", 42);
 *
 * // 값 조회
 * Object token = context.get("login", "token");  // "abc123"
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class VariableContext {

  private final Map<String, Map<String, Object>> stepVariables = new HashMap<>();

  /**
   * 변수를 저장한다.
   *
   * @param stepId   Step ID
   * @param varName  변수 이름
   * @param value    값
   */
  public void put(String stepId, String varName, Object value) {
    stepVariables
        .computeIfAbsent(stepId, k -> new HashMap<>())
        .put(varName, value);
  }

  /**
   * 변수를 조회한다.
   *
   * @param stepId  Step ID
   * @param varName 변수 이름
   * @return 값, 없으면 null
   */
  public Object get(String stepId, String varName) {
    Map<String, Object> vars = stepVariables.get(stepId);
    return vars != null ? vars.get(varName) : null;
  }

  /**
   * 변수를 Optional로 조회한다.
   *
   * @param stepId  Step ID
   * @param varName 변수 이름
   * @return 값 Optional
   */
  public Optional<Object> find(String stepId, String varName) {
    return Optional.ofNullable(get(stepId, varName));
  }

  /**
   * 변수 존재 여부를 확인한다.
   *
   * @param stepId  Step ID
   * @param varName 변수 이름
   * @return 존재하면 true
   */
  public boolean has(String stepId, String varName) {
    Map<String, Object> vars = stepVariables.get(stepId);
    return vars != null && vars.containsKey(varName);
  }

  /**
   * 특정 Step의 모든 변수를 반환한다.
   *
   * @param stepId Step ID
   * @return 변수 맵 (없으면 빈 맵)
   */
  public Map<String, Object> getStepVariables(String stepId) {
    Map<String, Object> vars = stepVariables.get(stepId);
    return vars != null ? Map.copyOf(vars) : Map.of();
  }

  /**
   * 모든 변수를 반환한다.
   *
   * @return 전체 변수 맵 (stepId → varName → value)
   */
  public Map<String, Map<String, Object>> getAll() {
    Map<String, Map<String, Object>> result = new HashMap<>();
    stepVariables.forEach((stepId, vars) -> result.put(stepId, Map.copyOf(vars)));
    return result;
  }

  /**
   * 모든 변수를 초기화한다.
   */
  public void clear() {
    stepVariables.clear();
  }
}