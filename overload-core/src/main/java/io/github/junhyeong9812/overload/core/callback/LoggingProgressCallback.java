package io.github.junhyeong9812.overload.core.callback;

import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 진행 상황을 로깅하는 기본 콜백 구현체.
 *
 * <p>Java 9+ System.Logger를 사용하여 진행 상황을 로깅한다.
 * 지정된 간격마다 로그를 출력하여 과도한 로깅을 방지한다.
 *
 * <p><b>특징:</b>
 * <ul>
 *   <li>System.Logger 사용 - SLF4J, Log4j 등과 자동 연동</li>
 *   <li>로깅 간격 설정 - 기본 10% 단위</li>
 *   <li>100% 완료 시 항상 로깅</li>
 *   <li>스레드 안전 - AtomicInteger 사용</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * // 기본 설정 (10% 단위)
 * ProgressCallback callback = new LoggingProgressCallback();
 *
 * // 5% 단위로 로깅
 * ProgressCallback callback = new LoggingProgressCallback(5);
 *
 * TestResult result = LoadTester.run(config, callback);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 * @see ProgressCallback
 */
public class LoggingProgressCallback implements ProgressCallback {

  private static final Logger logger = System.getLogger(LoggingProgressCallback.class.getName());
  private static final int DEFAULT_LOG_INTERVAL_PERCENT = 10;

  private final int logIntervalPercent;
  private final AtomicInteger lastLoggedIntervalIndex = new AtomicInteger(-1);

  /**
   * 기본 로깅 간격(10%)으로 LoggingProgressCallback을 생성한다.
   */
  public LoggingProgressCallback() {
    this(DEFAULT_LOG_INTERVAL_PERCENT);
  }

  /**
   * 지정된 로깅 간격으로 LoggingProgressCallback을 생성한다.
   *
   * @param logIntervalPercent 로깅 간격 (백분율, 1-100)
   * @throws IllegalArgumentException 로깅 간격이 1-100 범위를 벗어난 경우
   */
  public LoggingProgressCallback(int logIntervalPercent) {
    if (logIntervalPercent < 1 || logIntervalPercent > 100) {
      throw new IllegalArgumentException("Log interval must be between 1 and 100");
    }
    this.logIntervalPercent = logIntervalPercent;
  }

  /**
   * {@inheritDoc}
   *
   * <p>지정된 간격마다 진행 상황을 로깅한다.
   * 100% 완료 시 항상 로깅한다.
   * 스레드 안전하게 동작하며, 중복 로깅을 방지한다.
   *
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @param result    개별 요청 결과 (이 구현에서는 사용하지 않음)
   */
  @Override
  public void onProgress(int completed, int total, RequestResult result) {
    if (total <= 0) {
      return;
    }

    int currentPercent = (int) ((double) completed / total * 100);
    int currentIntervalIndex = currentPercent / logIntervalPercent;

    // 100% 완료 시 항상 로깅 (특별 처리)
    if (completed == total) {
      int lastIndex = lastLoggedIntervalIndex.get();
      int completeIndex = 100 / logIntervalPercent;
      if (lastIndex < completeIndex) {
        if (lastLoggedIntervalIndex.compareAndSet(lastIndex, completeIndex)) {
          logProgress(completed, total, currentPercent);
        }
      }
      return;
    }

    // CAS를 사용하여 중복 로깅 방지
    int lastIndex = lastLoggedIntervalIndex.get();
    if (currentIntervalIndex > lastIndex) {
      if (lastLoggedIntervalIndex.compareAndSet(lastIndex, currentIntervalIndex)) {
        logProgress(completed, total, currentPercent);
      }
    }
  }

  /**
   * 진행 상황을 로깅한다.
   *
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @param percent   진행률 (백분율)
   */
  private void logProgress(int completed, int total, int percent) {
    logger.log(Level.INFO, "Load test progress: {0}/{1} ({2}%)",
        completed, total, percent);
  }

  /**
   * 로깅 상태를 초기화한다.
   *
   * <p>새로운 테스트 시작 전에 호출하여 이전 테스트의 로깅 상태를 초기화한다.
   */
  public void reset() {
    lastLoggedIntervalIndex.set(-1);
  }
}