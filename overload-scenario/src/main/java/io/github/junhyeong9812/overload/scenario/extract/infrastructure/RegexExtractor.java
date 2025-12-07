package io.github.junhyeong9812.overload.scenario.extract.infrastructure;

import io.github.junhyeong9812.overload.core.http.domain.DetailedRequestResult;
import io.github.junhyeong9812.overload.scenario.extract.application.port.ValueExtractorPort;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정규식 기반 값 추출기.
 *
 * <p>응답 본문 또는 헤더에서 정규식으로 값을 추출한다.
 *
 * <p><b>경로 형식:</b>
 * <ul>
 *   <li>Body: {@code $regex.body.(pattern)}</li>
 *   <li>Header: {@code $regex.header.HeaderName.(pattern)}</li>
 * </ul>
 *
 * <p><b>예시:</b>
 * <ul>
 *   <li>{@code $regex.body.(token=([^&]+))} - Body에서 token 값 추출</li>
 *   <li>{@code $regex.header.Set-Cookie.(session=([^;]+))} - 쿠키에서 session 추출</li>
 * </ul>
 *
 * <p>패턴에 캡처 그룹이 있으면 첫 번째 그룹을, 없으면 전체 매치를 반환한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class RegexExtractor implements ValueExtractorPort {

  private static final String BODY_PREFIX = "$regex.body.";
  private static final String HEADER_PREFIX = "$regex.header.";

  /**
   * 정규식으로 값을 추출한다.
   *
   * @param response 상세 응답 결과
   * @param path     정규식 추출 경로
   * @return 추출된 값 (첫 번째 캡처 그룹 또는 전체 매치)
   */
  @Override
  public Optional<Object> extract(DetailedRequestResult.Success response, String path) {
    if (path.startsWith(BODY_PREFIX)) {
      return extractFromBody(response, path);
    } else if (path.startsWith(HEADER_PREFIX)) {
      return extractFromHeader(response, path);
    }
    return Optional.empty();
  }

  /**
   * $regex.로 시작하는 경로를 지원한다.
   *
   * @param path 추출 경로
   * @return $regex.로 시작하면 true
   */
  @Override
  public boolean supports(String path) {
    return path != null && (path.startsWith(BODY_PREFIX) || path.startsWith(HEADER_PREFIX));
  }

  /**
   * 응답 본문에서 정규식으로 값을 추출한다.
   */
  private Optional<Object> extractFromBody(DetailedRequestResult.Success response, String path) {
    String patternStr = extractPattern(path.substring(BODY_PREFIX.length()));
    String body = response.responseBody();

    if (body == null || body.isBlank()) {
      return Optional.empty();
    }

    return applyPattern(body, patternStr);
  }

  /**
   * 응답 헤더에서 정규식으로 값을 추출한다.
   */
  private Optional<Object> extractFromHeader(DetailedRequestResult.Success response, String path) {
    String remaining = path.substring(HEADER_PREFIX.length());
    int patternStart = remaining.indexOf('.');

    if (patternStart == -1) {
      return Optional.empty();
    }

    String headerName = remaining.substring(0, patternStart);
    String patternStr = extractPattern(remaining.substring(patternStart + 1));
    String headerValue = response.getHeader(headerName);

    if (headerValue == null) {
      return Optional.empty();
    }

    return applyPattern(headerValue, patternStr);
  }

  /**
   * 괄호로 감싸진 패턴을 추출한다.
   */
  private String extractPattern(String str) {
    if (str.startsWith("(") && str.endsWith(")")) {
      return str.substring(1, str.length() - 1);
    }
    return str;
  }

  /**
   * 정규식을 적용하여 값을 추출한다.
   */
  private Optional<Object> applyPattern(String text, String patternStr) {
    try {
      Pattern pattern = Pattern.compile(patternStr);
      Matcher matcher = pattern.matcher(text);

      if (matcher.find()) {
        // 캡처 그룹이 있으면 첫 번째 그룹, 없으면 전체 매치
        if (matcher.groupCount() > 0) {
          return Optional.ofNullable(matcher.group(1));
        }
        return Optional.of(matcher.group());
      }
    } catch (Exception e) {
      // 잘못된 정규식
    }
    return Optional.empty();
  }
}