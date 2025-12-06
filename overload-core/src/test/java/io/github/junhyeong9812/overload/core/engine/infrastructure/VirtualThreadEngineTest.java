package io.github.junhyeong9812.overload.core.engine.infrastructure;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.engine.domain.LoadTestEngine;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VirtualThreadEngine")
class VirtualThreadEngineTest {

  private VirtualThreadEngine engine;
  private MockHttpClient mockHttpClient;

  @BeforeEach
  void setUp() {
    mockHttpClient = new MockHttpClient();
    engine = new VirtualThreadEngine(mockHttpClient);
  }

  @Test
  @DisplayName("LoadTestEngine 인터페이스를 구현한다")
  void implementsLoadTestEngine() {
    assertThat(engine).isInstanceOf(LoadTestEngine.class);
  }

  @Nested
  @DisplayName("execute")
  class ExecuteTest {

    @Test
    @DisplayName("지정된 수만큼 요청을 실행한다")
    void executesSpecifiedNumberOfRequests() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      List<RequestResult> results = engine.execute(config, ProgressCallback.noop());

      assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("모든 결과를 반환한다")
    void returnsAllResults() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(5)
          .concurrency(2)
          .build();

      List<RequestResult> results = engine.execute(config, ProgressCallback.noop());

      assertThat(results).allMatch(r -> r instanceof RequestResult.Success);
    }

    @Test
    @DisplayName("콜백이 호출된다")
    void callbackIsCalled() {
      AtomicInteger callbackCount = new AtomicInteger(0);

      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      engine.execute(config, (completed, total) -> callbackCount.incrementAndGet());

      assertThat(callbackCount.get()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("동시성 제어")
  class ConcurrencyTest {

    @Test
    @DisplayName("동시 실행 수가 concurrency를 초과하지 않는다")
    void respectsConcurrencyLimit() {
      int concurrency = 5;
      ConcurrencyTrackingHttpClient trackingClient = new ConcurrencyTrackingHttpClient();
      VirtualThreadEngine trackingEngine = new VirtualThreadEngine(trackingClient);

      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(20)
          .concurrency(concurrency)
          .build();

      trackingEngine.execute(config, ProgressCallback.noop());

      assertThat(trackingClient.getMaxConcurrent()).isLessThanOrEqualTo(concurrency);
    }
  }

  /**
   * 테스트용 Mock HTTP 클라이언트
   */
  static class MockHttpClient implements HttpClientPort {

    @Override
    public RequestResult send(io.github.junhyeong9812.overload.core.http.domain.HttpRequest request) {
      return new RequestResult.Success(200, 10);
    }
  }

  /**
   * 동시 실행 수를 추적하는 HTTP 클라이언트
   */
  static class ConcurrencyTrackingHttpClient implements HttpClientPort {

    private final AtomicInteger currentConcurrent = new AtomicInteger(0);
    private final AtomicInteger maxConcurrent = new AtomicInteger(0);

    @Override
    public RequestResult send(io.github.junhyeong9812.overload.core.http.domain.HttpRequest request) {
      int current = currentConcurrent.incrementAndGet();
      maxConcurrent.updateAndGet(max -> Math.max(max, current));

      try {
        // 약간의 지연을 주어 동시성 추적 가능하게
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      currentConcurrent.decrementAndGet();
      return new RequestResult.Success(200, 10);
    }

    public int getMaxConcurrent() {
      return maxConcurrent.get();
    }
  }
}