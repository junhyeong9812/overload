package io.github.junhyeong9812.overload.core.http.domain;

/**
 * HTTP 요청 실패 유형.
 *
 * <p>요청 실패 시 원인을 분류한다.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public enum ErrorType {

  /**
   * 요청 타임아웃.
   */
  TIMEOUT,

  /**
   * 연결 거부 (서버가 연결을 거부함).
   */
  CONNECTION_REFUSED,

  /**
   * 연결 리셋 (Connection reset, Broken pipe 등).
   */
  CONNECTION_RESET,

  /**
   * 분류되지 않은 기타 오류.
   */
  UNKNOWN
}