package io.github.junhyeong9812.overload.starter.dto;

import java.util.List;

/**
 * WebSocket으로 전송되는 진행 상황 메시지 DTO.
 *
 * <p>부하 테스트의 실시간 진행 상황을 대시보드에 전달한다.
 * 테스트 ID, URL, 진행률, 상태, 최근 요청 로그 등을 포함한다.
 *
 * <p><b>JSON 출력 예시:</b>
 * <pre>{@code
 * {
 *   "testId": "abc12345",
 *   "url": "http://localhost:8080/api/test",
 *   "method": "GET",
 *   "completed": 50,
 *   "total": 100,
 *   "status": "RUNNING",
 *   "recentLogs": [...]
 * }
 * }</pre>
 *
 * @param testId     테스트 고유 ID
 * @param url        테스트 대상 URL
 * @param method     HTTP 메서드
 * @param completed  완료된 요청 수
 * @param total      전체 요청 수
 * @param status     테스트 상태 (RUNNING, COMPLETED, FAILED, CANCELLED)
 * @param recentLogs 최근 요청 로그 목록
 * @author junhyeong9812
 * @since 1.0.0
 */
public record ProgressMessage(
    String testId,
    String url,
    String method,
    int completed,
    int total,
    String status,
    List<RequestLog> recentLogs
) {

  /**
   * 진행률을 백분율로 계산한다.
   *
   * @return 진행률 (0.0 ~ 100.0)
   */
  public double percentage() {
    return total > 0 ? (double) completed / total * 100 : 0;
  }

  /**
   * 요청 로그 없이 ProgressMessage를 생성한다.
   *
   * <p>테스트 취소 등 로그가 필요 없는 상황에서 사용한다.
   *
   * @param testId    테스트 ID
   * @param url       테스트 대상 URL
   * @param method    HTTP 메서드
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @param status    테스트 상태
   * @return ProgressMessage 인스턴스
   */
  public static ProgressMessage of(String testId, String url, String method,
      int completed, int total, String status) {
    return new ProgressMessage(testId, url, method, completed, total, status, List.of());
  }

  /**
   * 요청 로그를 포함한 ProgressMessage를 생성한다.
   *
   * <p>실시간 요청 로그를 대시보드에 전달할 때 사용한다.
   *
   * @param testId    테스트 ID
   * @param url       테스트 대상 URL
   * @param method    HTTP 메서드
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @param status    테스트 상태
   * @param logs      최근 요청 로그 목록
   * @return ProgressMessage 인스턴스
   */
  public static ProgressMessage withLogs(String testId, String url, String method,
      int completed, int total, String status,
      List<RequestLog> logs) {
    return new ProgressMessage(testId, url, method, completed, total, status, logs);
  }
}