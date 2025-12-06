package io.github.junhyeong9812.overload.cli.command;

import io.github.junhyeong9812.overload.cli.output.JsonFormatter;
import io.github.junhyeong9812.overload.cli.output.OutputFormatter;
import io.github.junhyeong9812.overload.cli.output.TextFormatter;
import io.github.junhyeong9812.overload.cli.progress.ConsoleProgressBar;
import io.github.junhyeong9812.overload.core.LoadTester;
import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 부하 테스트 실행 커맨드.
 *
 * <p>사용 예시:
 * <pre>
 * overload run -u https://httpbin.org/get -c 100 -n 1000
 * overload run -u https://api.example.com -X POST -H "Content-Type: application/json" -d '{"test":true}'
 * </pre>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
//@Command(
//    name = "run",
//    description = "부하 테스트를 실행합니다",
//    mixinStandardHelpOptions = true
//)
@Command(
    name = "run",
    description = "Execute load test",
    mixinStandardHelpOptions = true
)
public class RunCommand implements Callable<Integer> {

  @Option(
      names = {"-u", "--url"},
      description = "Target URL (required)",
      required = true
  )
  private String url;

  @Option(
      names = {"-X", "--method"},
      description = "HTTP method (default: ${DEFAULT-VALUE})",
      defaultValue = "GET"
  )
  private HttpMethod method;

  @Option(
      names = {"-c", "--concurrency"},
      description = "Concurrent requests (default: ${DEFAULT-VALUE})",
      defaultValue = "10"
  )
  private int concurrency;

  @Option(
      names = {"-n", "--requests"},
      description = "Total requests (default: ${DEFAULT-VALUE})",
      defaultValue = "100"
  )
  private int totalRequests;

  @Option(
      names = {"-H", "--header"},
      description = "HTTP header (e.g. -H \"Content-Type: application/json\")"
  )
  private List<String> headers;

  @Option(
      names = {"-d", "--data"},
      description = "Request body"
  )
  private String body;

  @Option(
      names = {"--timeout"},
      description = "Request timeout in seconds (default: ${DEFAULT-VALUE})",
      defaultValue = "5"
  )
  private int timeout;

  @Option(
      names = {"--json"},
      description = "Output result as JSON"
  )
  private boolean jsonOutput;

  @Option(
      names = {"-q", "--quiet"},
      description = "Disable progress bar"
  )
  private boolean quiet;
//  @Option(
//      names = {"-u", "--url"},
//      description = "테스트 대상 URL (필수)",
//      required = true
//  )
//  private String url;
//
//  @Option(
//      names = {"-X", "--method"},
//      description = "HTTP 메서드 (기본값: ${DEFAULT-VALUE})",
//      defaultValue = "GET"
//  )
//  private HttpMethod method;
//
//  @Option(
//      names = {"-c", "--concurrency"},
//      description = "동시 요청 수 (기본값: ${DEFAULT-VALUE})",
//      defaultValue = "10"
//  )
//  private int concurrency;
//
//  @Option(
//      names = {"-n", "--requests"},
//      description = "총 요청 수 (기본값: ${DEFAULT-VALUE})",
//      defaultValue = "100"
//  )
//  private int totalRequests;
//
//  @Option(
//      names = {"-H", "--header"},
//      description = "HTTP 헤더 (예: -H \"Content-Type: application/json\")"
//  )
//  private List<String> headers;
//
//  @Option(
//      names = {"-d", "--data"},
//      description = "요청 본문"
//  )
//  private String body;
//
//  @Option(
//      names = {"--timeout"},
//      description = "요청 타임아웃 초 (기본값: ${DEFAULT-VALUE})",
//      defaultValue = "5"
//  )
//  private int timeout;
//
//  @Option(
//      names = {"--json"},
//      description = "결과를 JSON 형식으로 출력"
//  )
//  private boolean jsonOutput;
//
//  @Option(
//      names = {"-q", "--quiet"},
//      description = "진행률 표시 비활성화"
//  )
//  private boolean quiet;

  @Override
  public Integer call() {
    try {
      // 설정 빌드
      LoadTestConfig config = buildConfig();

      // 포매터 선택
      OutputFormatter formatter = jsonOutput
          ? new JsonFormatter()
          : new TextFormatter();

      // 헤더 출력 (JSON 모드가 아닐 때만)
      if (!jsonOutput) {
        printHeader(config);
      }

      // 프로그레스 콜백 설정
      ProgressCallback callback = createProgressCallback();

      // 테스트 실행
      TestResult result = LoadTester.run(config, callback);

      // 프로그레스 바 종료 후 줄바꿈
      if (!quiet && !jsonOutput) {
        System.out.println();
        System.out.println();
      }

      // 결과 출력
      System.out.println(formatter.format(result));

      return 0;

    } catch (IllegalArgumentException e) {
      System.err.println("오류: " + e.getMessage());
      return 1;
    } catch (Exception e) {
      System.err.println("예기치 않은 오류: " + e.getMessage());
      e.printStackTrace();
      return 2;
    }
  }

  /**
   * LoadTestConfig를 빌드한다.
   */
  private LoadTestConfig buildConfig() {
    LoadTestConfig.Builder builder = LoadTestConfig.builder()
        .url(url)
        .method(method)
        .concurrency(concurrency)
        .totalRequests(totalRequests)
        .timeout(Duration.ofSeconds(timeout));

    // 헤더 파싱
    if (headers != null) {
      for (String header : headers) {
        String[] parts = header.split(":", 2);
        if (parts.length == 2) {
          builder.header(parts[0].trim(), parts[1].trim());
        }
      }
    }

    // 바디 설정
    if (body != null) {
      builder.body(body);
    }

    return builder.build();
  }

  /**
   * 테스트 시작 전 헤더를 출력한다.
   */
  private void printHeader(LoadTestConfig config) {
    System.out.println();
    System.out.println("Overload v1.0.0 - Virtual Thread Load Tester");
    System.out.println("=".repeat(50));
    System.out.println();
    System.out.printf("  Target:        %s%n", config.url());
    System.out.printf("  Method:        %s%n", config.method());
    System.out.printf("  Concurrency:   %d virtual threads%n", config.concurrency());
    System.out.printf("  Requests:      %,d%n", config.totalRequests());
    System.out.printf("  Timeout:       %ds%n", config.timeout().toSeconds());
    System.out.println();
  }

  /**
   * 진행률 콜백을 생성한다.
   */
  private ProgressCallback createProgressCallback() {
    if (quiet || jsonOutput) {
      return ProgressCallback.noop();
    }
    return new ConsoleProgressBar(totalRequests);
  }
}