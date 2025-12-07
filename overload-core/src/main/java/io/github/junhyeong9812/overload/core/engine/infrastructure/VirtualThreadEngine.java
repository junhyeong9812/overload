package io.github.junhyeong9812.overload.core.engine.infrastructure;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.engine.domain.ExecutionContext;
import io.github.junhyeong9812.overload.core.engine.domain.LoadTestEngine;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Java 21 Virtual Thread 기반의 부하 테스트 엔진 구현체.
 *
 * <p>Virtual Thread를 사용하여 대량의 동시 HTTP 요청을 효율적으로 처리한다.
 * Semaphore를 통해 동시 요청 수를 제어한다.
 *
 * <p><b>특징:</b>
 * <ul>
 *   <li>Virtual Thread 기반 - 수천 개의 동시 요청 처리 가능</li>
 *   <li>Semaphore 기반 동시성 제어</li>
 *   <li>취소 지원 - ExecutionContext를 통한 취소 처리</li>
 *   <li>진행률 콜백 - 각 요청 완료 시 개별 결과와 함께 콜백 호출</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * HttpClientPort httpClient = new JdkHttpClient(Duration.ofSeconds(5));
 * LoadTestEngine engine = new VirtualThreadEngine(httpClient);
 *
 * LoadTestConfig config = LoadTestConfig.builder()
 *     .url("https://api.example.com")
 *     .concurrency(100)
 *     .totalRequests(10000)
 *     .build();
 *
 * List<RequestResult> results = engine.execute(config, (completed, total, result) -> {
 *     System.out.printf("Progress: %d/%d%n", completed, total);
 *     if (result instanceof RequestResult.Success s) {
 *         System.out.println("  Status: " + s.statusCode());
 *     }
 * });
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see LoadTestEngine
 */
public class VirtualThreadEngine implements LoadTestEngine {

  private final HttpClientPort httpClient;

  /**
   * 지정된 HTTP 클라이언트로 VirtualThreadEngine을 생성한다.
   *
   * @param httpClient HTTP 요청을 수행할 클라이언트
   */
  public VirtualThreadEngine(HttpClientPort httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Virtual Thread를 사용하여 요청을 병렬로 실행한다.
   * Semaphore를 통해 동시 실행 수를 제한한다.
   *
   * <p>각 요청 완료 시 콜백이 호출되며, 개별 요청 결과가 함께 전달된다.
   * InterruptedException 발생 시 해당 요청은 결과에 포함되지 않으며,
   * 콜백도 호출되지 않는다.
   *
   * @param config   부하 테스트 설정
   * @param callback 진행 상황 및 개별 요청 결과를 받을 콜백
   * @return 모든 요청의 결과 목록
   */
  @Override
  public List<RequestResult> execute(LoadTestConfig config, ProgressCallback callback) {
    List<RequestResult> results = new CopyOnWriteArrayList<>();
    ExecutionContext context = new ExecutionContext(config.totalRequests());
    Semaphore semaphore = new Semaphore(config.concurrency());

    HttpRequest request = HttpRequest.from(
        config.url(),
        config.method(),
        config.headers(),
        config.body()
    );

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<?>> futures = new ArrayList<>();

      for (int i = 0; i < config.totalRequests(); i++) {
        futures.add(executor.submit(() -> executeRequest(
            request, results, context, semaphore, config.totalRequests(), callback
        )));
      }

      waitForCompletion(futures);
    }

    return results;
  }

  /**
   * 개별 HTTP 요청을 실행한다.
   *
   * <p>Semaphore를 획득한 후 요청을 수행하고, 결과를 기록한 후 콜백을 호출한다.
   *
   * @param request       실행할 HTTP 요청
   * @param results       결과를 저장할 리스트
   * @param context       실행 컨텍스트 (취소 상태 및 완료 카운트)
   * @param semaphore     동시성 제어용 세마포어
   * @param totalRequests 전체 요청 수
   * @param callback      진행 상황 콜백
   */
  private void executeRequest(
      HttpRequest request,
      List<RequestResult> results,
      ExecutionContext context,
      Semaphore semaphore,
      int totalRequests,
      ProgressCallback callback) {

    if (context.isCancelled()) {
      return;
    }

    boolean acquired = false;
    try {
      semaphore.acquire();
      acquired = true;

      RequestResult result = httpClient.send(request);
      results.add(result);

      int completed = context.incrementAndGetCompleted();
      callback.onProgress(completed, totalRequests, result);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (acquired) {
        semaphore.release();
      }
    }
  }

  /**
   * 모든 요청의 완료를 대기한다.
   *
   * @param futures 대기할 Future 목록
   */
  private void waitForCompletion(List<Future<?>> futures) {
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        // 개별 실패는 무시 (이미 results에 기록됨)
      }
    }
  }
}