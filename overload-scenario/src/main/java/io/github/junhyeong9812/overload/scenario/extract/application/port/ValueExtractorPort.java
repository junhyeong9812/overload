package io.github.junhyeong9812.overload.scenario.extract.application.port;

import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;

import java.util.Optional;

/**
 * 응답에서 값을 추출하는 포트 인터페이스.
 *
 * <p>다양한 추출 방식(JSONPath, Header, Regex)을 지원하기 위한 추상화이다.
 *
 * <p><b>지원 경로 형식:</b>
 * <ul>
 *   <li>JSONPath: {@code $.data.token}</li>
 *   <li>Header: {@code $header.Set-Cookie}</li>
 *   <li>Regex (Body): {@code $regex.body.(pattern)}</li>
 *   <li>Regex (Header): {@code $regex.header.Name.(pattern)}</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public interface ValueExtractorPort {

  /**
   * 응답에서 값을 추출한다.
   *
   * @param response 상세 응답 결과
   * @param path     추출 경로
   * @return 추출된 값 (없으면 Optional.empty())
   */
  Optional<Object> extract(DetailedRequestResult.Success response, String path);

  /**
   * 이 추출기가 해당 경로를 처리할 수 있는지 확인한다.
   *
   * @param path 추출 경로
   * @return 처리 가능하면 true
   */
  boolean supports(String path);
}