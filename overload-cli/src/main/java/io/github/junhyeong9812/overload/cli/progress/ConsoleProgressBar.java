package io.github.junhyeong9812.overload.cli.progress;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;

/**
 * 콘솔 프로그레스 바 구현.
 *
 * <p>터미널에 진행률을 시각적으로 표시한다.
 *
 * <pre>
 * Running... ████████████████░░░░░░░░░░░░░░░░░░░░░░░░  40% (400/1000)
 * </pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ConsoleProgressBar implements ProgressCallback {

  private static final int BAR_WIDTH = 40;
  private static final char FILLED_CHAR = '█';
  private static final char EMPTY_CHAR = '░';

  private final int total;
  private volatile int lastPercent = -1;

  public ConsoleProgressBar(int total) {
    this.total = total;
  }

  @Override
  public void onProgress(int completed, int total) {
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
}