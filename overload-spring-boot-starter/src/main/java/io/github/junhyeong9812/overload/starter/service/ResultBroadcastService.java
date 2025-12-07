package io.github.junhyeong9812.overload.starter.service;

import io.github.junhyeong9812.overload.starter.dto.ProgressMessage;
import io.github.junhyeong9812.overload.starter.dto.RequestLog;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket을 통한 실시간 진행 상황 브로드캐스트 서비스.
 *
 * <p>연결된 모든 WebSocket 클라이언트에게 부하 테스트 진행 상황을 브로드캐스트한다.
 * 스레드 안전하게 설계되어 동시 다중 연결을 지원한다.
 *
 * <p><b>주요 기능:</b>
 * <ul>
 *   <li>WebSocket 세션 관리</li>
 *   <li>진행 상황 메시지 브로드캐스트</li>
 *   <li>JSON 직렬화</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ResultBroadcastService {

  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

  /**
   * WebSocket 세션을 등록한다.
   *
   * @param session 등록할 세션
   */
  public void addSession(WebSocketSession session) {
    sessions.add(session);
  }

  /**
   * WebSocket 세션을 제거한다.
   *
   * @param session 제거할 세션
   */
  public void removeSession(WebSocketSession session) {
    sessions.remove(session);
  }

  /**
   * 진행 상황 메시지를 모든 연결된 클라이언트에게 브로드캐스트한다.
   *
   * <p>닫힌 세션은 자동으로 제거되며, 전송 실패는 무시한다.
   *
   * @param message 브로드캐스트할 메시지
   */
  public void broadcast(ProgressMessage message) {
    String json = toJson(message);
    TextMessage textMessage = new TextMessage(json);

    // 닫힌 세션 정리
    sessions.removeIf(session -> !session.isOpen());

    sessions.forEach(session -> {
      try {
        if (session.isOpen()) {
          synchronized (session) {
            session.sendMessage(textMessage);
          }
        }
      } catch (IOException e) {
        // 전송 실패 무시
      }
    });
  }

  /**
   * ProgressMessage를 JSON 문자열로 변환한다.
   *
   * @param message 변환할 메시지
   * @return JSON 문자열
   */
  private String toJson(ProgressMessage message) {
    String logsJson = "[]";
    if (message.recentLogs() != null && !message.recentLogs().isEmpty()) {
      logsJson = message.recentLogs().stream()
          .map(this::logToJson)
          .collect(Collectors.joining(",", "[", "]"));
    }

    return """
        {"testId":"%s","url":"%s","method":"%s","completed":%d,"total":%d,"status":"%s","percentage":%.1f,"recentLogs":%s}"""
        .formatted(
            escapeJson(message.testId()),
            escapeJson(message.url() != null ? message.url() : ""),
            escapeJson(message.method() != null ? message.method() : "GET"),
            message.completed(),
            message.total(),
            message.status(),
            message.percentage(),
            logsJson
        );
  }

  /**
   * RequestLog를 JSON 문자열로 변환한다.
   *
   * @param log 변환할 로그
   * @return JSON 문자열
   */
  private String logToJson(RequestLog log) {
    return """
        {"requestNumber":%d,"success":%s,"statusCode":%d,"latencyMs":%d,"error":%s}"""
        .formatted(
            log.requestNumber(),
            log.success(),
            log.statusCode(),
            log.latencyMs(),
            log.error() != null ? "\"" + escapeJson(log.error()) + "\"" : "null"
        );
  }

  /**
   * JSON 문자열에서 특수 문자를 이스케이프한다.
   *
   * @param str 이스케이프할 문자열
   * @return 이스케이프된 문자열
   */
  private String escapeJson(String str) {
    if (str == null) return "";
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}