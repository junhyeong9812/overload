package io.github.junhyeong9812.overload.core;

import io.github.junhyeong9812.overload.core.callback.LoggingProgressCallback;
import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link LoadTester} 테스트.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@DisplayName("LoadTester")
class LoadTesterTest {

  @Nested
  @DisplayName("run - 기본 실행")
  class RunTest {

    @Test
    @DisplayName("설정만으로 실행할 수 있다")
    void runWithConfigOnly() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      TestResult result = LoadTester.run(config, ProgressCallback.noop(), mockClient);

      assertThat(result.totalRequests()).isEqualTo(10);
    }

    @Test
    @DisplayName("콜백과 함께 실행할 수 있다")
    void runWithCallback() {
      AtomicInteger callbackCount = new AtomicInteger(0);

      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      LoadTester.run(config, (completed, total, result) ->
          callbackCount.incrementAndGet(), mockClient);

      assertThat(callbackCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("simple 콜백과 함께 실행할 수 있다")
    void runWithSimpleCallback() {
      AtomicInteger callbackCount = new AtomicInteger(0);

      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      LoadTester.run(config, ProgressCallback.simple((completed, total) ->
          callbackCount.incrementAndGet()), mockClient);

      assertThat(callbackCount.get()).isEqualTo(10);
    }

    @Test
    @DisplayName("LoggingProgressCallback과 함께 실행할 수 있다")
    void runWithLoggingCallback() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      TestResult result = LoadTester.run(config, new LoggingProgressCallback(), mockClient);

      assertThat(result.totalRequests()).isEqualTo(10);
    }

    @Test
    @DisplayName("콜백에서 RequestResult에 접근할 수 있다")
    void callbackReceivesRequestResult() {
      AtomicInteger successStatusSum = new AtomicInteger(0);

      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(5)
          .concurrency(2)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      LoadTester.run(config, (completed, total, result) -> {
        if (result instanceof RequestResult.Success success) {
          successStatusSum.addAndGet(success.statusCode());
        }
      }, mockClient);

      assertThat(successStatusSum.get()).isEqualTo(1000); // 200 * 5
    }
  }

  @Nested
  @DisplayName("결과 검증")
  class ResultTest {

    @Test
    @DisplayName("모든 요청 성공 시 성공률 100%")
    void allSuccessResult() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(100)
          .concurrency(10)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      TestResult result = LoadTester.run(config, ProgressCallback.noop(), mockClient);

      assertThat(result.successRate()).isCloseTo(100.0, within(0.01));
      assertThat(result.failCount()).isZero();
    }

    @Test
    @DisplayName("HTTP 5xx는 실패로 카운트")
    void serverErrorCountsAsFail() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      MockHttpClient mockClient = new MockHttpClient(500);

      TestResult result = LoadTester.run(config, ProgressCallback.noop(), mockClient);

      assertThat(result.successCount()).isZero();
      assertThat(result.failCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("네트워크 실패는 Failure로 기록")
    void networkFailureRecorded() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(10)
          .concurrency(5)
          .build();

      FailingHttpClient failingClient = new FailingHttpClient();

      TestResult result = LoadTester.run(config, ProgressCallback.noop(), failingClient);

      assertThat(result.successCount()).isZero();
      assertThat(result.failCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("지연 시간 통계가 계산된다")
    void latencyStatsCalculated() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(100)
          .concurrency(10)
          .build();

      VariableLatencyHttpClient variableClient = new VariableLatencyHttpClient();

      TestResult result = LoadTester.run(config, ProgressCallback.noop(), variableClient);

      assertThat(result.latencyStats().min()).isGreaterThanOrEqualTo(0);
      assertThat(result.latencyStats().max()).isGreaterThan(0);
      assertThat(result.latencyStats().avg()).isGreaterThan(0);
    }

    @Test
    @DisplayName("RPS가 계산된다")
    void rpsCalculated() {
      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(100)
          .concurrency(10)
          .build();

      MockHttpClient mockClient = new MockHttpClient(200);

      TestResult result = LoadTester.run(config, ProgressCallback.noop(), mockClient);

      assertThat(result.requestsPerSecond()).isGreaterThan(0);
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

      LoadTestConfig config = LoadTestConfig.builder()
          .url("https://api.example.com")
          .totalRequests(50)
          .concurrency(concurrency)
          .build();

      LoadTester.run(config, ProgressCallback.noop(), trackingClient);

      assertThat(trackingClient.getMaxConcurrent()).isLessThanOrEqualTo(concurrency);
    }
  }

  // ===== Test Doubles =====

  /**
   * 항상 지정된 상태 코드를 반환하는 Mock.
   */
  static class MockHttpClient implements HttpClientPort {
    private final int statusCode;

    MockHttpClient(int statusCode) {
      this.statusCode = statusCode;
    }

    @Override
    public RequestResult send(HttpRequest request) {
      return new RequestResult.Success(statusCode, 10);
    }
  }

  /**
   * 항상 실패하는 Mock.
   */
  static class FailingHttpClient implements HttpClientPort {
    @Override
    public RequestResult send(HttpRequest request) {
      return new RequestResult.Failure(
          "Connection refused",
          RequestResult.ErrorType.CONNECTION_REFUSED,
          100
      );
    }
  }

  /**
   * 가변 지연 시간을 반환하는 Mock.
   */
  static class VariableLatencyHttpClient implements HttpClientPort {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public RequestResult send(HttpRequest request) {
      int latency = counter.incrementAndGet() % 100 + 1;
      return new RequestResult.Success(200, latency);
    }
  }

  /**
   * 동시 실행 수를 추적하는 Mock.
   */
  static class ConcurrencyTrackingHttpClient implements HttpClientPort {
    private final AtomicInteger currentConcurrent = new AtomicInteger(0);
    private final AtomicInteger maxConcurrent = new AtomicInteger(0);

    @Override
    public RequestResult send(HttpRequest request) {
      int current = currentConcurrent.incrementAndGet();
      maxConcurrent.updateAndGet(max -> Math.max(max, current));

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      currentConcurrent.decrementAndGet();
      return new RequestResult.Success(200, 10);
    }

    int getMaxConcurrent() {
      return maxConcurrent.get();
    }
  }
}