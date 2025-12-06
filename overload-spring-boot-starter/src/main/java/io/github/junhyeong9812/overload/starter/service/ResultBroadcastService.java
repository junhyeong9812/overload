package io.github.junhyeong9812.overload.starter.service;

import io.github.junhyeong9812.overload.starter.dto.ProgressMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket broadcast service for real-time progress updates.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ResultBroadcastService {

  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

  public void addSession(WebSocketSession session) {
    sessions.add(session);
  }

  public void removeSession(WebSocketSession session) {
    sessions.remove(session);
  }

  public void broadcast(ProgressMessage message) {
    String json = toJson(message);
    TextMessage textMessage = new TextMessage(json);

    sessions.removeIf(session -> !session.isOpen());

    sessions.forEach(session -> {
      try {
        if (session.isOpen()) {
          synchronized (session) {
            session.sendMessage(textMessage);
          }
        }
      } catch (IOException e) {
        // Ignore send failures
      }
    });
  }

  private String toJson(ProgressMessage message) {
    return """
                {"testId":"%s","completed":%d,"total":%d,"status":"%s","percentage":%.1f}"""
        .formatted(
            message.testId(),
            message.completed(),
            message.total(),
            message.status(),
            message.percentage()
        );
  }
}