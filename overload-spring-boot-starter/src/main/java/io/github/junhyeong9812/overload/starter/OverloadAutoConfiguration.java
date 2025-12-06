package io.github.junhyeong9812.overload.starter;

import io.github.junhyeong9812.overload.starter.controller.OverloadApiController;
import io.github.junhyeong9812.overload.starter.controller.OverloadDashboardController;
import io.github.junhyeong9812.overload.starter.service.LoadTestService;
import io.github.junhyeong9812.overload.starter.service.ResultBroadcastService;
import io.github.junhyeong9812.overload.starter.websocket.OverloadWebSocketHandler;
import io.github.junhyeong9812.overload.starter.websocket.WebSocketConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Overload auto-configuration.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(OverloadProperties.class)
@ConditionalOnProperty(
    prefix = "overload",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Import(WebSocketConfig.class)
public class OverloadAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ResultBroadcastService resultBroadcastService() {
    return new ResultBroadcastService();
  }

  @Bean
  @ConditionalOnMissingBean
  public LoadTestService loadTestService(
      OverloadProperties properties,
      ResultBroadcastService broadcastService) {
    return new LoadTestService(properties, broadcastService);
  }

  @Bean
  @ConditionalOnMissingBean
  public OverloadWebSocketHandler overloadWebSocketHandler(
      ResultBroadcastService broadcastService) {
    return new OverloadWebSocketHandler(broadcastService);
  }

  @Bean
  @ConditionalOnMissingBean
  public OverloadDashboardController overloadDashboardController(
      OverloadProperties properties) {
    return new OverloadDashboardController(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public OverloadApiController overloadApiController(
      LoadTestService loadTestService) {
    return new OverloadApiController(loadTestService);
  }
}