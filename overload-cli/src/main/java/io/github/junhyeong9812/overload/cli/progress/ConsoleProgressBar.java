package io.github.junhyeong9812.overload.cli.progress;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

/**
 * 콘솔 프로그레스 바 구현.
 *
 * <p>터미널에 진행률을 시각적으로 표시한다.
 * 같은 퍼센트에서는 출력을 스킵하여 과도한 출력을 방지한다.
 *
 * <pre>
 * Running... ████████████████░░░░░░░░░░░░░░░░░░░░░░░░  40% (400/1,000)
 * </pre>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * LoadTestConfig config = LoadTestConfig.builder()
 *     .url("https://api.example.com")
 *     .totalRequests(1000)
 *     .build();
 *
 * ProgressCallback progressBar = new ConsoleProgressBar(config.totalRequests());
 * TestResult result = LoadTester.run(config, progressBar);
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ConsoleProgressBar implements ProgressCallback {

  /** 프로그레스 바의 너비 (문자 수) */
  private static final int BAR_WIDTH = 40;

  /** 완료된 부분을 나타내는 문자 */
  private static final char FILLED_CHAR = '█';

  /** 미완료 부분을 나타내는 문자 */
  private static final char EMPTY_CHAR = '░';

  private final int total;
  private volatile int lastPercent = -1;

  /**
   * ConsoleProgressBar를 생성한다.
   *
   * @param total 전체 요청 수
   */
  public ConsoleProgressBar(int total) {
    this.total = total;
  }

  /**
   * {@inheritDoc}
   *
   * <p>진행률을 콘솔에 프로그레스 바 형태로 출력한다.
   * 같은 퍼센트에서는 출력을 스킵하여 성능을 최적화한다.
   * 개별 요청 결과({@code result})는 사용하지 않는다.
   *
   * @param completed 완료된 요청 수
   * @param total     전체 요청 수
   * @param result    개별 요청 결과 (이 구현에서는 사용하지 않음)
   */
  @Override
  public void onProgress(int completed, int total, RequestResult result) {
    int percent = (int) ((double) completed / total * 100);

    // 같은 퍼센트면 스킵 (과도한 출력 방지)
    if (percent == lastPercent && completed != total) {
      return;
    }
    lastPercent = percent;

    int filled = (int) ((double) completed / total * BAR_WIDTH);
    int empty = BAR_WIDTH - filled;

    StringBuilder bar = new StringBuilder();
    bar.append("\rRunning... ");

    // 프로그레스 바
    bar.append(String.valueOf(FILLED_CHAR).repeat(filled));
    bar.append(String.valueOf(EMPTY_CHAR).repeat(empty));

    // 퍼센트 및 카운트
    bar.append(String.format(" %3d%% (%,d/%,d)", percent, completed, total));

    System.out.print(bar);
    System.out.flush();
  }

  /**
   * 프로그레스 바 출력을 완료하고 줄바꿈을 출력한다.
   *
   * <p>테스트 완료 후 호출하여 다음 출력이 새 줄에서 시작되도록 한다.
   */
  public void complete() {
    System.out.println();
  }

  /**
   * 프로그레스 바 상태를 초기화한다.
   *
   * <p>새로운 테스트 시작 전에 호출하여 이전 상태를 초기화한다.
   */
  public void reset() {
    lastPercent = -1;
  }
}