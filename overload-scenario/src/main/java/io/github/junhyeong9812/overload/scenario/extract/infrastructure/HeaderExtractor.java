package io.github.junhyeong9812.overload.scenario.extract.infrastructure;

import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;
import io.github.junhyeong9812.overload.scenario.extract.application.port.ValueExtractorPort;

import java.util.Optional;

/**
 * HTTP 헤더 기반 값 추출기.
 *
 * <p>응답 헤더에서 값을 추출한다.
 *
 * <p><b>경로 형식:</b> {@code $header.HeaderName}
 *
 * <p><b>예시:</b>
 * <ul>
 *   <li>{@code $header.Set-Cookie}</li>
 *   <li>{@code $header.Location}</li>
 *   <li>{@code $header.X-Request-Id}</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class HeaderExtractor implements ValueExtractorPort {

  private static final String PREFIX = "$header.";

  /**
   * 응답 헤더에서 값을 추출한다.
   *
   * @param response 상세 응답 결과
   * @param path     헤더 추출 경로 ($header.HeaderName)
   * @return 추출된 헤더 값
   */
  @Override
  public Optional<Object> extract(DetailedRequestResult.Success response, String path) {
    String headerName = path.substring(PREFIX.length());
    String value = response.getHeader(headerName);
    return Optional.ofNullable(value);
  }

  /**
   * $header.로 시작하는 경로를 지원한다.
   *
   * @param path 추출 경로
   * @return $header.로 시작하면 true
   */
  @Override
  public boolean supports(String path) {
    return path != null && path.startsWith(PREFIX);
  }
}