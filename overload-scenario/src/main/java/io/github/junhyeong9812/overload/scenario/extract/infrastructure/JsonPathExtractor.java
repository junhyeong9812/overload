package io.github.junhyeong9812.overload.scenario.extract.infrastructure;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;
import io.github.junhyeong9812.overload.scenario.extract.application.port.ValueExtractorPort;

import java.util.Optional;

/**
 * JSONPath 기반 값 추출기.
 *
 * <p>응답 본문에서 JSONPath 표현식으로 값을 추출한다.
 *
 * <p><b>지원 경로 예시:</b>
 * <ul>
 *   <li>{@code $.data.token} - 중첩 필드</li>
 *   <li>{@code $.users[0].id} - 배열 인덱스</li>
 *   <li>{@code $.items[-1]} - 마지막 요소</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class JsonPathExtractor implements ValueExtractorPort {

  /**
   * JSONPath로 응답 본문에서 값을 추출한다.
   *
   * @param response 상세 응답 결과
   * @param path     JSONPath 표현식 ($.로 시작)
   * @return 추출된 값
   */
  @Override
  public Optional<Object> extract(DetailedRequestResult.Success response, String path) {
    if (response.responseBody() == null || response.responseBody().isBlank()) {
      return Optional.empty();
    }

    try {
      Object value = JsonPath.read(response.responseBody(), path);
      return Optional.ofNullable(value);
    } catch (PathNotFoundException e) {
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * $.로 시작하고 $header, $regex가 아닌 경로를 지원한다.
   *
   * @param path 추출 경로
   * @return JSONPath 형식이면 true
   */
  @Override
  public boolean supports(String path) {
    return path != null
        && path.startsWith("$")
        && !path.startsWith("$header")
        && !path.startsWith("$regex");
  }
}