package io.github.junhyeong9812.overload.starter.websocket;

import io.github.junhyeong9812.overload.starter.service.ResultBroadcastService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for real-time progress updates.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class OverloadWebSocketHandler extends TextWebSocketHandler {

  private final ResultBroadcastService broadcastService;

  public OverloadWebSocketHandler(ResultBroadcastService broadcastService) {
    this.broadcastService = broadcastService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    broadcastService.addSession(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    broadcastService.removeSession(session);
  }
}