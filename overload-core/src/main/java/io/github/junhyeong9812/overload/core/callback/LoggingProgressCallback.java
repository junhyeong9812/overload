package io.github.junhyeong9812.overload.core.callback;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

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
  private volatile int lastLoggedPercent = -1;

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
   */
  @Override
  public void onProgress(int completed, int total) {
    if (total <= 0) {
      return;
    }

    int currentPercent = (int) ((double) completed / total * 100);

    // 100% 완료 시 항상 로깅
    if (completed == total) {
      logProgress(completed, total, currentPercent);
      return;
    }

    // 간격 단위로 로깅 (중복 방지)
    int intervalIndex = currentPercent / logIntervalPercent;
    int lastIntervalIndex = lastLoggedPercent / logIntervalPercent;

    if (intervalIndex > lastIntervalIndex || lastLoggedPercent < 0) {
      logProgress(completed, total, currentPercent);
      lastLoggedPercent = currentPercent;
    }
  }

  /**
   * 진행 상황을 로깅한다.
   */
  private void logProgress(int completed, int total, int percent) {
    logger.log(Level.INFO, "Load test progress: {0}/{1} ({2}%)",
        completed, total, percent);
  }

  /**
   * 로깅 상태를 초기화한다.
   *
   * <p>새로운 테스트 시작 전에 호출할 수 있다.
   */
  public void reset() {
    lastLoggedPercent = -1;
  }
}