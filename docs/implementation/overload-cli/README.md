# overload-cli 구현 계획

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | overload-cli |
| Java 버전 | 25 |
| 주요 의존성 | picocli, jansi, snakeyaml |
| 아키텍처 | 단순 구조 (MVC 없음) |
| 목적 | 커맨드라인 인터페이스 |

---

## 왜 단순한 구조인가?

CLI는 Core를 호출하는 **"얇은 래퍼"**입니다.

```java
// CLI의 전체 흐름 - 매우 단순
public class RunCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        // 1. 옵션 파싱 (picocli가 해줌)
        // 2. LoadTestConfig 생성
        // 3. LoadTester.run(config) 호출  ← 로직은 core에 있음
        // 4. 결과 출력
    }
}
```

- 복잡한 비즈니스 로직 없음 (모든 로직은 core에)
- picocli를 다른 CLI 프레임워크로 교체할 일이 거의 없음
- Hexagonal은 오버 엔지니어링

---

## 폴더 구조

```
overload-cli/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── io/github/junhyeong9812/overload/cli/
    │   │       │
    │   │       ├── Main.java                   # 진입점
    │   │       │
    │   │       ├── command/                    # CLI 명령어
    │   │       │   ├── RootCommand.java
    │   │       │   └── RunCommand.java
    │   │       │
    │   │       ├── config/                     # 설정 로더
    │   │       │   └── YamlConfigLoader.java
    │   │       │
    │   │       ├── output/                     # 출력 포매터
    │   │       │   ├── OutputFormatter.java
    │   │       │   ├── TextFormatter.java
    │   │       │   ├── JsonFormatter.java
    │   │       │   └── CsvFormatter.java
    │   │       │
    │   │       └── progress/                   # 진행률 표시
    │   │           └── ProgressBar.java
    │   │
    │   └── resources/
    │
    └── test/
        └── java/
            └── io/github/junhyeong9812/overload/cli/
```

---

## 의존성

### build.gradle

```groovy
plugins {
    id 'java'
    id 'application'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = 'io.github.junhyeong9812.overload.cli.Main'
}

dependencies {
    // Core 모듈
    implementation project(':overload-core')
    
    // CLI 프레임워크
    implementation 'info.picocli:picocli:4.7.6'
    annotationProcessor 'info.picocli:picocli-codegen:4.7.6'
    
    // YAML 파싱
    implementation 'org.yaml:snakeyaml:2.3'
    
    // 터미널 색상
    implementation 'org.fusesource.jansi:jansi:2.4.1'
    
    // 테스트
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.3'
    testImplementation 'org.assertj:assertj-core:3.26.3'
}

// Fat JAR 생성
tasks.register('fatJar', Jar) {
    archiveClassifier = 'all'
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes 'Main-Class': application.mainClass
    }
    
    from {
        configurations.runtimeClasspath.collect { 
            it.isDirectory() ? it : zipTree(it) 
        }
    }
    
    with jar
}

build.dependsOn fatJar
```

---

## 구현 상세

### 1. Main.java

```java
package io.github.junhyeong9812.overload.cli;

import io.github.junhyeong9812.overload.cli.command.RootCommand;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

public class Main {
    
    public static void main(String[] args) {
        // ANSI 색상 지원 활성화
        AnsiConsole.systemInstall();
        
        try {
            int exitCode = new CommandLine(new RootCommand())
                .execute(args);
            System.exit(exitCode);
        } finally {
            AnsiConsole.systemUninstall();
        }
    }
}
```

---

### 2. command 패키지

#### RootCommand.java

```java
package io.github.junhyeong9812.overload.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "overload",
    description = "Lightweight HTTP load testing tool powered by Virtual Threads",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    subcommands = {
        RunCommand.class
    }
)
public class RootCommand implements Runnable {
    
    @Option(names = {"-v", "--version"}, 
            versionHelp = true, 
            description = "Print version information")
    private boolean versionRequested;
    
    @Override
    public void run() {
        System.out.println();
        System.out.println("Overload v0.1.0 - Virtual Thread Load Tester");
        System.out.println();
        System.out.println("Usage: overload <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  run     Execute HTTP load test");
        System.out.println();
        System.out.println("Use 'overload <command> --help' for more information.");
    }
}
```

#### RunCommand.java

```java
package io.github.junhyeong9812.overload.cli.command;

import io.github.junhyeong9812.overload.cli.config.YamlConfigLoader;
import io.github.junhyeong9812.overload.cli.output.JsonFormatter;
import io.github.junhyeong9812.overload.cli.output.OutputFormatter;
import io.github.junhyeong9812.overload.cli.output.TextFormatter;
import io.github.junhyeong9812.overload.cli.progress.ProgressBar;
import io.github.junhyeong9812.overload.core.LoadTester;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "run",
    description = "Execute HTTP load test",
    mixinStandardHelpOptions = true
)
public class RunCommand implements Callable<Integer> {
    
    @Option(names = {"-u", "--url"}, 
            description = "Target URL (required unless -f is specified)")
    private String url;
    
    @Option(names = {"-m", "--method"}, 
            defaultValue = "GET",
            description = "HTTP method (GET, POST, PUT, DELETE, PATCH)")
    private String method;
    
    @Option(names = {"-c", "--concurrency"}, 
            defaultValue = "10",
            description = "Number of concurrent requests")
    private int concurrency;
    
    @Option(names = {"-n", "--requests"}, 
            defaultValue = "100",
            description = "Total number of requests")
    private int totalRequests;
    
    @Option(names = {"--timeout"}, 
            defaultValue = "5000",
            description = "Request timeout in milliseconds")
    private int timeout;
    
    @Option(names = {"-H", "--header"}, 
            description = "HTTP headers (can be repeated)")
    private List<String> headers = new ArrayList<>();
    
    @Option(names = {"-d", "--data"}, 
            description = "Request body")
    private String body;
    
    @Option(names = {"-f", "--file"}, 
            description = "YAML configuration file")
    private File configFile;
    
    @Option(names = {"-o", "--output"}, 
            defaultValue = "text",
            description = "Output format (text, json)")
    private String outputFormat;
    
    @Option(names = {"-q", "--quiet"}, 
            description = "Minimal output")
    private boolean quiet;
    
    @Option(names = {"--no-color"}, 
            description = "Disable colored output")
    private boolean noColor;
    
    @Override
    public Integer call() {
        try {
            // 설정 로드
            LoadTestConfig config = loadConfig();
            
            // 헤더 출력
            if (!quiet) {
                printHeader(config);
            }
            
            // 프로그레스 바
            ProgressBar progressBar = new ProgressBar(config.totalRequests(), noColor);
            
            // 테스트 실행
            TestResult result = LoadTester.run(config, (completed, total) -> {
                if (!quiet) {
                    progressBar.update(completed);
                }
            });
            
            if (!quiet) {
                progressBar.complete();
            }
            
            // 결과 출력
            OutputFormatter formatter = createFormatter();
            System.out.println(formatter.format(result));
            
            return 0;
            
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (!quiet) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private LoadTestConfig loadConfig() {
        // YAML 파일이 지정된 경우
        if (configFile != null) {
            return YamlConfigLoader.load(configFile);
        }
        
        // URL 필수 체크
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                "URL is required. Use -u <url> or -f <config.yaml>");
        }
        
        // CLI 옵션으로 설정 생성
        LoadTestConfig.Builder builder = LoadTestConfig.builder()
            .url(url)
            .method(HttpMethod.valueOf(method.toUpperCase()))
            .concurrency(concurrency)
            .totalRequests(totalRequests)
            .timeout(Duration.ofMillis(timeout));
        
        // 헤더 파싱
        for (String header : headers) {
            String[] parts = header.split(":", 2);
            if (parts.length == 2) {
                builder.header(parts[0].trim(), parts[1].trim());
            }
        }
        
        // 요청 본문
        if (body != null) {
            builder.body(body);
        }
        
        return builder.build();
    }
    
    private void printHeader(LoadTestConfig config) {
        System.out.println();
        System.out.println("Overload v0.1.0 - Virtual Thread Load Tester");
        System.out.println();
        System.out.println("Target:        " + config.url());
        System.out.println("Method:        " + config.method());
        System.out.println("Concurrency:   " + config.concurrency() + " virtual threads");
        System.out.println("Requests:      " + config.totalRequests());
        System.out.println();
    }
    
    private OutputFormatter createFormatter() {
        return switch (outputFormat.toLowerCase()) {
            case "json" -> new JsonFormatter();
            default -> new TextFormatter(noColor);
        };
    }
}
```

---

### 3. config 패키지

#### YamlConfigLoader.java

```java
package io.github.junhyeong9812.overload.cli.config;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlConfigLoader {
    
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    
    public static LoadTestConfig load(File file) {
        try (var input = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            return parseConfig(config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + file, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static LoadTestConfig parseConfig(Map<String, Object> config) {
        LoadTestConfig.Builder builder = LoadTestConfig.builder();
        
        // target 섹션
        Map<String, Object> target = (Map<String, Object>) config.get("target");
        if (target != null) {
            builder.url(resolveEnv((String) target.get("url")));
            
            String method = (String) target.get("method");
            if (method != null) {
                builder.method(HttpMethod.valueOf(method.toUpperCase()));
            }
            
            Map<String, String> headers = (Map<String, String>) target.get("headers");
            if (headers != null) {
                headers.forEach((k, v) -> builder.header(k, resolveEnv(v)));
            }
            
            String body = (String) target.get("body");
            if (body != null) {
                builder.body(resolveEnv(body));
            }
        }
        
        // load 섹션
        Map<String, Object> load = (Map<String, Object>) config.get("load");
        if (load != null) {
            Integer concurrency = (Integer) load.get("concurrency");
            if (concurrency != null) {
                builder.concurrency(concurrency);
            }
            
            Integer requests = (Integer) load.get("requests");
            if (requests != null) {
                builder.totalRequests(requests);
            }
        }
        
        // options 섹션
        Map<String, Object> options = (Map<String, Object>) config.get("options");
        if (options != null) {
            Integer timeout = (Integer) options.get("timeout");
            if (timeout != null) {
                builder.timeout(Duration.ofMillis(timeout));
            }
        }
        
        return builder.build();
    }
    
    private static String resolveEnv(String value) {
        if (value == null) return null;
        
        Matcher matcher = ENV_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String envName = matcher.group(1);
            String envValue = System.getenv(envName);
            matcher.appendReplacement(result, envValue != null ? envValue : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
```

---

### 4. output 패키지

#### OutputFormatter.java

```java
package io.github.junhyeong9812.overload.cli.output;

import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

public interface OutputFormatter {
    
    String format(TestResult result);
}
```

#### TextFormatter.java

```java
package io.github.junhyeong9812.overload.cli.output;

import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

public class TextFormatter implements OutputFormatter {
    
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    
    private final boolean noColor;
    
    public TextFormatter() {
        this(false);
    }
    
    public TextFormatter(boolean noColor) {
        this.noColor = noColor;
    }
    
    @Override
    public String format(TestResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(colorize("Results:", CYAN)).append("\n");
        sb.append(String.format("  Total Requests:    %,d%n", result.totalRequests()));
        
        // 성공률에 따른 색상
        String successColor = result.successRate() >= 99 ? GREEN : 
                              result.successRate() >= 95 ? YELLOW : RED;
        sb.append(String.format("  Successful:        %s%,d (%.1f%%)%s%n",
            colorize("", successColor),
            result.successCount(), 
            result.successRate(),
            colorize("", RESET)));
        
        sb.append(String.format("  Failed:            %,d (%.1f%%)%n",
            result.failCount(), 
            result.failRate()));
        
        sb.append(String.format("  Total Time:        %.2fs%n",
            result.totalDuration().toMillis() / 1000.0));
        
        sb.append(String.format("  Requests/sec:      %s%.2f%s%n",
            colorize("", CYAN),
            result.requestsPerSecond(),
            colorize("", RESET)));
        
        sb.append("\n");
        
        var latency = result.latencyStats();
        sb.append(colorize("Latency Distribution:", CYAN)).append("\n");
        sb.append(String.format("  Min:       %dms%n", latency.min()));
        sb.append(String.format("  Max:       %dms%n", latency.max()));
        sb.append(String.format("  Avg:       %.0fms%n", latency.avg()));
        sb.append("\n");
        
        var p = latency.percentiles();
        sb.append(String.format("  p50:       %dms%n", p.p50()));
        sb.append(String.format("  p90:       %dms%n", p.p90()));
        sb.append(String.format("  p95:       %dms%n", p.p95()));
        sb.append(String.format("  p99:       %dms%n", p.p99()));
        
        return sb.toString();
    }
    
    private String colorize(String text, String color) {
        if (noColor) {
            return text;
        }
        return color + text + RESET;
    }
}
```

#### JsonFormatter.java

```java
package io.github.junhyeong9812.overload.cli.output;

import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

public class JsonFormatter implements OutputFormatter {
    
    @Override
    public String format(TestResult result) {
        var latency = result.latencyStats();
        var percentiles = latency.percentiles();
        
        return String.format("""
            {
              "totalRequests": %d,
              "successCount": %d,
              "failCount": %d,
              "successRate": %.2f,
              "totalDurationMs": %d,
              "requestsPerSecond": %.2f,
              "latency": {
                "min": %d,
                "max": %d,
                "avg": %.2f,
                "p50": %d,
                "p90": %d,
                "p95": %d,
                "p99": %d
              }
            }""",
            result.totalRequests(),
            result.successCount(),
            result.failCount(),
            result.successRate(),
            result.totalDuration().toMillis(),
            result.requestsPerSecond(),
            latency.min(),
            latency.max(),
            latency.avg(),
            percentiles.p50(),
            percentiles.p90(),
            percentiles.p95(),
            percentiles.p99()
        );
    }
}
```

---

### 5. progress 패키지

#### ProgressBar.java

```java
package io.github.junhyeong9812.overload.cli.progress;

public class ProgressBar {
    
    private static final int WIDTH = 40;
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    
    private final int total;
    private final boolean noColor;
    private int lastPrinted = -1;
    
    public ProgressBar(int total) {
        this(total, false);
    }
    
    public ProgressBar(int total, boolean noColor) {
        this.total = total;
        this.noColor = noColor;
    }
    
    public void update(int current) {
        // 1% 단위로만 업데이트 (성능 최적화)
        int percent = (int) ((double) current / total * 100);
        if (percent == lastPrinted) {
            return;
        }
        lastPrinted = percent;
        
        double percentage = (double) current / total;
        int filled = (int) (WIDTH * percentage);
        
        StringBuilder bar = new StringBuilder("\r");
        bar.append("Running... ");
        
        if (!noColor) {
            bar.append(GREEN);
        }
        
        bar.append("[");
        for (int i = 0; i < WIDTH; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        
        if (!noColor) {
            bar.append(RESET);
        }
        
        bar.append(String.format(" %3d%% (%,d/%,d)", percent, current, total));
        
        System.out.print(bar);
        System.out.flush();
    }
    
    public void complete() {
        update(total);
        System.out.println();
        System.out.println();
    }
}
```

---

## 실행 흐름

```
┌─────────────────────────────────────────────────────────┐
│  $ overload run -u https://... -c 100 -n 1000           │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  Main.java                                              │
│  • AnsiConsole 초기화                                   │
│  • CommandLine 실행                                     │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  RunCommand.java                                        │
│  • 옵션 파싱 (picocli)                                  │
│  • YAML 로드 또는 CLI 옵션으로 설정 생성               │
│  • LoadTestConfig 생성                                  │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  LoadTester.run(config, callback)                       │
│  (overload-core)                                        │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  ProgressBar.update() ← callback                        │
│  결과 수신                                              │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  OutputFormatter.format(result)                         │
│  • TextFormatter → 터미널 출력                          │
│  • JsonFormatter → JSON 출력                            │
└─────────────────────────────────────────────────────────┘
```

---

## 구현 체크리스트

- [ ] Main.java
- [ ] command 패키지
    - [ ] RootCommand
    - [ ] RunCommand
- [ ] config 패키지
    - [ ] YamlConfigLoader
- [ ] output 패키지
    - [ ] OutputFormatter 인터페이스
    - [ ] TextFormatter
    - [ ] JsonFormatter
- [ ] progress 패키지
    - [ ] ProgressBar
- [ ] Fat JAR 빌드 설정
- [ ] 테스트