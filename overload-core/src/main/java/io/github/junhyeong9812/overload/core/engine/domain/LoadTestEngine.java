package io.github.junhyeong9812.overload.core.engine.domain;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

import java.util.List;

/**
 * 부하 테스트 엔진 인터페이스.
 *
 * <p>설정에 따라 HTTP 요청을 동시에 실행하고 결과를 수집한다.
 * 다양한 실행 전략(Virtual Thread, Platform Thread 등)을 구현할 수 있다.
 *
 * <p><b>구현체:</b>
 * <ul>
 *   <li>{@code VirtualThreadEngine} - Java 21 Virtual Thread 기반 (기본)</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * LoadTestEngine engine = new VirtualThreadEngine(httpClient);
 *
 * List<RequestResult> results = engine.execute(config, (completed, total) ->
 *     System.out.printf("Progress: %d/%d%n", completed, total)
 * );
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see io.github.junhyeong9812.overload.core.engine.infrastructure.VirtualThreadEngine
 */
public interface LoadTestEngine {

  /**
   * 부하 테스트를 실행한다.
   *
   * <p>설정에 지정된 동시성과 총 요청 수에 따라 HTTP 요청을 실행한다.
   * 각 요청 완료 시 콜백이 호출된다.
   *
   * @param config   테스트 설정
   * @param callback 진행 상황 콜백
   * @return 모든 요청 결과 목록
   */
  List<RequestResult> execute(LoadTestConfig config, ProgressCallback callback);
}