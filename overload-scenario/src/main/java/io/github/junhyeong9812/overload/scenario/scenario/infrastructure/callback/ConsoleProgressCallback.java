package io.github.junhyeong9812.overload.scenario.scenario.infrastructure.callback;

import io.github.junhyeong9812.overload.scenario.scenario.application.callback.ScenarioProgressCallback;
import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioResult;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 콘솔에 시나리오 진행 상황을 출력하는 콜백 구현체.
 *
 * <p>부하 테스트 진행 중 실시간 상태를 콘솔에 표시한다.
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ScenarioTestResult result = ScenarioLoadTester.run(
 *     scenario, 100, 10, timeout,
 *     new ConsoleProgressCallback()
 * );
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ConsoleProgressCallback implements ScenarioProgressCallback {

  private final PrintStream out;
  private final AtomicInteger successCount = new AtomicInteger(0);
  private final AtomicInteger failCount = new AtomicInteger(0);
  private final int updateInterval;

  /**
   * 기본 설정으로 생성한다.
   *
   * <p>매 10회마다 진행 상황을 출력한다.
   */
  public ConsoleProgressCallback() {
    this(System.out, 10);
  }

  /**
   * 업데이트 간격을 지정하여 생성한다.
   *
   * @param updateInterval 진행 상황 출력 간격 (N회마다 출력)
   */
  public ConsoleProgressCallback(int updateInterval) {
    this(System.out, updateInterval);
  }

  /**
   * 출력 스트림과 업데이트 간격을 지정하여 생성한다.
   *
   * @param out            출력 스트림
   * @param updateInterval 진행 상황 출력 간격
   */
  public ConsoleProgressCallback(PrintStream out, int updateInterval) {
    this.out = out;
    this.updateInterval = Math.max(1, updateInterval);
  }

  @Override
  public void onProgress(int completed, int total, ScenarioResult result) {
    if (result.success()) {
      successCount.incrementAndGet();
    } else {
      failCount.incrementAndGet();
    }

    // 일정 간격마다 출력
    if (completed % updateInterval == 0 || completed == total) {
      double progress = (double) completed / total * 100;
      double successRate = completed > 0
          ? (double) successCount.get() / completed * 100
          : 0;

      out.printf("\r[%3.0f%%] %d/%d completed | Success: %d (%.1f%%) | Failed: %d",
          progress,
          completed,
          total,
          successCount.get(),
          successRate,
          failCount.get()
      );

      if (completed == total) {
        out.println();  // 완료 시 줄바꿈
      }
    }
  }
}