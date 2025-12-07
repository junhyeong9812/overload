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
    sendToAll(json);
  }

  // ==================== 시나리오 테스트용 메서드 추가 ====================

  /**
   * 시나리오 진행 상황을 브로드캐스트한다.
   *
   * @param testId   테스트 ID
   * @param progress 진행 상황 데이터
   */
  public void broadcastScenarioProgress(String testId, Object progress) {
    String json = """
            {"type":"SCENARIO_PROGRESS","testId":"%s","data":%s}"""
        .formatted(escapeJson(testId), objectToJson(progress));
    sendToAll(json);
  }

  /**
   * 시나리오 완료를 브로드캐스트한다.
   *
   * @param testId 테스트 ID
   * @param result 최종 결과 데이터
   */
  public void broadcastScenarioComplete(String testId, Object result) {
    String json = """
            {"type":"SCENARIO_COMPLETE","testId":"%s","data":%s}"""
        .formatted(escapeJson(testId), objectToJson(result));
    sendToAll(json);
  }

  /**
   * 시나리오 에러를 브로드캐스트한다.
   *
   * @param testId       테스트 ID
   * @param errorMessage 에러 메시지
   */
  public void broadcastScenarioError(String testId, String errorMessage) {
    String json = """
            {"type":"SCENARIO_ERROR","testId":"%s","error":"%s"}"""
        .formatted(escapeJson(testId), escapeJson(errorMessage));
    sendToAll(json);
  }

  // ==================== 기존 private 메서드 ====================

  /**
   * 모든 세션에 JSON 메시지를 전송한다.
   *
   * @param json 전송할 JSON 문자열
   */
  private void sendToAll(String json) {
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
   * 객체를 JSON 문자열로 변환한다 (시나리오용).
   *
   * @param obj 변환할 객체
   * @return JSON 문자열
   */
  private String objectToJson(Object obj) {
    if (obj == null) return "null";

    // record 타입의 경우 리플렉션으로 JSON 생성
    if (obj.getClass().isRecord()) {
      return recordToJson(obj);
    }

    return "\"" + escapeJson(obj.toString()) + "\"";
  }

  /**
   * Record를 JSON으로 변환한다.
   *
   * @param record 변환할 record
   * @return JSON 문자열
   */
  private String recordToJson(Object record) {
    try {
      var components = record.getClass().getRecordComponents();
      StringBuilder sb = new StringBuilder("{");

      for (int i = 0; i < components.length; i++) {
        if (i > 0) sb.append(",");

        String name = components[i].getName();
        Object value = components[i].getAccessor().invoke(record);

        sb.append("\"").append(name).append("\":");

        if (value == null) {
          sb.append("null");
        } else if (value instanceof String) {
          sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
          sb.append(value);
        } else if (value instanceof java.util.List<?> list) {
          sb.append(listToJson(list));
        } else if (value.getClass().isRecord()) {
          sb.append(recordToJson(value));
        } else {
          sb.append("\"").append(escapeJson(value.toString())).append("\"");
        }
      }

      sb.append("}");
      return sb.toString();
    } catch (Exception e) {
      return "{}";
    }
  }

  /**
   * List를 JSON 배열로 변환한다.
   *
   * @param list 변환할 리스트
   * @return JSON 배열 문자열
   */
  private String listToJson(java.util.List<?> list) {
    if (list.isEmpty()) return "[]";

    return list.stream()
        .map(item -> {
          if (item == null) return "null";
          if (item instanceof String) return "\"" + escapeJson((String) item) + "\"";
          if (item instanceof Number || item instanceof Boolean) return item.toString();
          if (item.getClass().isRecord()) return recordToJson(item);
          return "\"" + escapeJson(item.toString()) + "\"";
        })
        .collect(Collectors.joining(",", "[", "]"));
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