package io.github.junhyeong9812.overload.core;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.engine.infrastructure.VirtualThreadEngine;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.http.infrastructure.JdkHttpClient;
import io.github.junhyeong9812.overload.core.metric.application.MetricAggregator;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

import java.util.List;

/**
 * 부하 테스트 실행 Facade.
 *
 * <p>부하 테스트 실행을 위한 간단한 진입점을 제공한다.
 * 내부적으로 HTTP 클라이언트, 실행 엔진, 메트릭 수집기를 조율한다.
 *
 * <p><b>간단한 사용:</b>
 * <pre>{@code
 * LoadTestConfig config = LoadTestConfig.builder()
 *     .url("https://api.example.com")
 *     .concurrency(100)
 *     .totalRequests(10000)
 *     .build();
 *
 * TestResult result = LoadTester.run(config);
 *
 * System.out.println("Success Rate: " + result.successRate() + "%");
 * System.out.println("RPS: " + result.requestsPerSecond());
 * }</pre>
 *
 * <p><b>콜백과 함께 사용:</b>
 * <pre>{@code
 * TestResult result = LoadTester.run(config, (completed, total) ->
 *     System.out.printf("\rProgress: %d/%d", completed, total)
 * );
 * }</pre>
 *
 * <p><b>커스텀 HTTP 클라이언트 사용:</b>
 * <pre>{@code
 * HttpClientPort customClient = new OkHttpClientAdapter();
 * TestResult result = LoadTester.run(config, callback, customClient);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public final class LoadTester {

  private LoadTester() {
    // Utility class - 인스턴스화 방지
  }

  /**
   * 부하 테스트를 실행한다.
   *
   * <p>기본 설정으로 실행하며, 진행 상황 콜백은 무시된다.
   *
   * @param config 테스트 설정
   * @return 테스트 결과
   */
  public static TestResult run(LoadTestConfig config) {
    return run(config, ProgressCallback.noop());
  }

  /**
   * 부하 테스트를 실행한다.
   *
   * <p>기본 JDK HTTP 클라이언트를 사용한다.
   *
   * @param config   테스트 설정
   * @param callback 진행 상황 콜백
   * @return 테스트 결과
   */
  public static TestResult run(LoadTestConfig config, ProgressCallback callback) {
    HttpClientPort httpClient = new JdkHttpClient(config.timeout());
    return run(config, callback, httpClient);
  }

  /**
   * 부하 테스트를 실행한다.
   *
   * <p>커스텀 HTTP 클라이언트를 사용할 수 있다.
   *
   * @param config     테스트 설정
   * @param callback   진행 상황 콜백
   * @param httpClient HTTP 클라이언트
   * @return 테스트 결과
   */
  public static TestResult run(
      LoadTestConfig config,
      ProgressCallback callback,
      HttpClientPort httpClient) {

    VirtualThreadEngine engine = new VirtualThreadEngine(httpClient);
    MetricAggregator aggregator = new MetricAggregator();

    aggregator.start();
    List<RequestResult> results = engine.execute(config, callback);
    aggregator.end();

    for (RequestResult result : results) {
      aggregator.record(result);
    }

    return aggregator.aggregate();
  }
}