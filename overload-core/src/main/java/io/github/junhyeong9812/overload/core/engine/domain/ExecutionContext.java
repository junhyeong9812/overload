package io.github.junhyeong9812.overload.core.engine.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 부하 테스트 실행 컨텍스트.
 *
 * <p>테스트 실행 중 상태를 관리한다. 진행 상황 추적, 취소 처리 등을 담당한다.
 * 스레드 안전하게 설계되어 여러 Virtual Thread에서 동시 접근 가능하다.
 *
 * <p><b>관리 상태:</b>
 * <ul>
 *   <li>총 요청 수</li>
 *   <li>완료된 요청 수</li>
 *   <li>취소 여부</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * ExecutionContext context = new ExecutionContext(1000);
 *
 * // 요청 완료 시
 * int completed = context.incrementAndGetCompleted();
 * System.out.println("Progress: " + context.getProgress() + "%");
 *
 * // 취소 처리
 * if (shouldCancel) {
 *     context.cancel();
 * }
 * }</pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
public class ExecutionContext {

  private final int totalRequests;
  private final AtomicInteger completedCount = new AtomicInteger(0);
  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  /**
   * 지정된 총 요청 수로 ExecutionContext를 생성한다.
   *
   * @param totalRequests 총 요청 수
   */
  public ExecutionContext(int totalRequests) {
    this.totalRequests = totalRequests;
  }

  /**
   * 총 요청 수를 반환한다.
   *
   * @return 총 요청 수
   */
  public int getTotalRequests() {
    return totalRequests;
  }

  /**
   * 현재까지 완료된 요청 수를 반환한다.
   *
   * @return 완료된 요청 수
   */
  public int getCompletedCount() {
    return completedCount.get();
  }

  /**
   * 완료된 요청 수를 1 증가시키고 증가된 값을 반환한다.
   *
   * @return 증가 후 완료된 요청 수
   */
  public int incrementAndGetCompleted() {
    return completedCount.incrementAndGet();
  }

  /**
   * 테스트가 취소되었는지 확인한다.
   *
   * @return 취소되었으면 {@code true}
   */
  public boolean isCancelled() {
    return cancelled.get();
  }

  /**
   * 테스트를 취소한다.
   */
  public void cancel() {
    cancelled.set(true);
  }

  /**
   * 현재 진행률을 반환한다.
   *
   * @return 진행률 (0.0 ~ 100.0)
   */
  public double getProgress() {
    return totalRequests > 0
        ? (double) completedCount.get() / totalRequests * 100
        : 0;
  }
}