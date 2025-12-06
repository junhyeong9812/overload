package io.github.junhyeong9812.overload.starter.websocket;

import io.github.junhyeong9812.overload.starter.OverloadProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final OverloadWebSocketHandler handler;
  private final OverloadProperties properties;

  public WebSocketConfig(OverloadWebSocketHandler handler, OverloadProperties properties) {
    this.handler = handler;
    this.properties = properties;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    String wsPath = properties.getDashboard().getPath() + "/ws";
    registry.addHandler(handler, wsPath)
        .setAllowedOrigins("*");
  }
}
