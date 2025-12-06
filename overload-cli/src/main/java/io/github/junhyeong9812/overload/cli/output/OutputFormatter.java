package io.github.junhyeong9812.overload.cli.output;

import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

/**
 * 테스트 결과 출력 포매터 인터페이스.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public interface OutputFormatter {

  /**
   * 테스트 결과를 문자열로 포맷팅한다.
   *
   * @param result 테스트 결과
   * @return 포맷팅된 문자열
   */
  String format(TestResult result);
}