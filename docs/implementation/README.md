# 구현 계획

이 문서는 Overload 프로젝트의 단계별 구현 계획과 상세 설계를 담고 있습니다.

---

## 목차

1. [개발 환경](#개발-환경)
2. [Phase 1: Core + CLI](#phase-1-core--cli)
3. [Phase 2: Advanced Features](#phase-2-advanced-features)
4. [Phase 3: Web Dashboard](#phase-3-web-dashboard)
5. [도메인 모델](#도메인-모델)
6. [기술적 고려사항](#기술적-고려사항)

---

## 개발 환경

### 기술 스택

| 모듈 | 기술 | 버전 |
|------|------|------|
| **overload-core** | Java (Virtual Threads) | 25 |
| | JDK HttpClient | 내장 |
| **overload-cli** | picocli | 4.7.5 |
| | SnakeYAML | 2.2 |
| | jansi (터미널 색상) | 2.4.1 |
| **overload-web** | Spring Boot | 4.0.0 |
| | Thymeleaf | 3.1.x |
| **Build** | Gradle | 8.x |

### 멀티 모듈 Gradle 설정

#### settings.gradle

```groovy
rootProject.name = 'overload'

include 'overload-core'
include 'overload-cli'
include 'overload-web'
```

#### 루트 build.gradle

```groovy
plugins {
    id 'java'
}

allprojects {
    group = 'io.github.junhyeong9812'
    version = '0.1.0-SNAPSHOT'

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    tasks.withType(JavaCompile).configureEach {
        options.encoding = 'UTF-8'
    }

    test {
        useJUnitPlatform()
    }
}
```

#### overload-core/build.gradle

```groovy
plugins {
    id 'java-library'
}

description = 'Overload Core Engine'

dependencies {
    // 외부 의존성 없음 - JDK만 사용
    
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.assertj:assertj-core:3.24.2'
}
```

#### overload-cli/build.gradle

```groovy
plugins {
    id 'application'
}

description = 'Overload CLI'

application {
    mainClass = 'io.github.junhyeong9812.overload.cli.Main'
}

dependencies {
    implementation project(':overload-core')
    
    implementation 'info.picocli:picocli:4.7.5'
    annotationProcessor 'info.picocli:picocli-codegen:4.7.5'
    implementation 'org.yaml:snakeyaml:2.2'
    implementation 'org.fusesource.jansi:jansi:2.4.1'
    
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

jar {
    manifest {
        attributes 'Main-Class': application.mainClass
    }
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

---

## Phase 1: Core + CLI

### 목표

**동작하는 CLI 부하 테스트 도구 완성**

```bash
# 이렇게 동작해야 함
$ overload run -u https://httpbin.org/get -c 100 -n 1000

Target:        https://httpbin.org/get
Concurrency:   100
Requests:      1,000

Running... ████████████████████ 100%

Results:
  Successful:      985 (98.5%)
  Failed:          15 (1.5%)
  Requests/sec:    427.35
  Avg Latency:     156ms
  p99 Latency:     720ms
```

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 단일 URL 테스트 | GET/POST/PUT/DELETE 요청 | P0 |
| 동시성 설정 | `-c` 옵션으로 동시 요청 수 | P0 |
| 요청 수 설정 | `-n` 옵션으로 총 요청 수 | P0 |
| 결과 리포트 | 성공률, TPS, Latency, Percentiles | P0 |
| 진행률 표시 | 터미널 프로그레스 바 | P0 |
| 헤더 설정 | `-H` 옵션으로 커스텀 헤더 | P1 |
| 요청 본문 | `-d` 옵션으로 요청 바디 | P1 |
| 타임아웃 | `-t` 옵션으로 타임아웃 설정 | P1 |

---

### overload-core 설계

#### 패키지 구조

```
overload-core/
└── src/main/java/io/github/junhyeong9812/overload/core/
    │
    ├── engine/                         # 테스트 실행 엔진
    │   ├── domain/
    │   │   ├── LoadTestEngine.java     # 엔진 인터페이스
    │   │   └── ExecutionContext.java   # 실행 컨텍스트
    │   ├── application/
    │   │   └── TestExecutor.java       # 실행 서비스
    │   └── infrastructure/
    │       └── VirtualThreadEngine.java
    │
    ├── http/                           # HTTP 클라이언트
    │   ├── domain/
    │   │   ├── HttpRequest.java
    │   │   ├── HttpResponse.java
    │   │   └── RequestResult.java
    │   ├── application/
    │   │   └── port/
    │   │       └── HttpClientPort.java
    │   └── infrastructure/
    │       └── JdkHttpClient.java
    │
    ├── metric/                         # 메트릭 계산
    │   ├── domain/
    │   │   ├── TestResult.java
    │   │   └── Percentiles.java
    │   └── application/
    │       └── MetricAggregator.java
    │
    ├── config/                         # 설정
    │   ├── LoadTestConfig.java
    │   └── HttpMethod.java
    │
    └── LoadTester.java                 # Facade (진입점)
```

#### 핵심 클래스

##### LoadTestConfig (설정)

```java
public record LoadTestConfig(
    String url,
    HttpMethod method,
    int concurrency,
    int totalRequests,
    Duration timeout,
    Map<String, String> headers,
    String body
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String url;
        private HttpMethod method = HttpMethod.GET;
        private int concurrency = 10;
        private int totalRequests = 100;
        private Duration timeout = Duration.ofSeconds(5);
        private Map<String, String> headers = new HashMap<>();
        private String body;
        
        public Builder url(String url) { this.url = url; return this; }
        public Builder method(HttpMethod method) { this.method = method; return this; }
        public Builder concurrency(int concurrency) { this.concurrency = concurrency; return this; }
        public Builder totalRequests(int totalRequests) { this.totalRequests = totalRequests; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder header(String key, String value) { this.headers.put(key, value); return this; }
        public Builder body(String body) { this.body = body; return this; }
        
        public LoadTestConfig build() {
            Objects.requireNonNull(url, "URL is required");
            if (concurrency < 1) throw new IllegalArgumentException("Concurrency must be >= 1");
            if (totalRequests < 1) throw new IllegalArgumentException("Total requests must be >= 1");
            return new LoadTestConfig(url, method, concurrency, totalRequests, timeout, headers, body);
        }
    }
}
```

##### TestResult (결과)

```java
public record TestResult(
    int totalRequests,
    int successCount,
    int failCount,
    Duration totalDuration,
    double requestsPerSecond,
    LatencyStats latencyStats
) {
    public double successRate() {
        return totalRequests > 0 ? (double) successCount / totalRequests * 100 : 0;
    }
    
    public record LatencyStats(
        long min,
        long max,
        double avg,
        Percentiles percentiles
    ) {}
}

public record Percentiles(
    long p50,
    long p90,
    long p95,
    long p99
) {}
```

##### RequestResult (개별 요청 결과)

```java
public sealed interface RequestResult 
    permits RequestResult.Success, RequestResult.Failure {
    
    record Success(int statusCode, long latencyMs) implements RequestResult {}
    
    record Failure(String errorMessage, ErrorType errorType) implements RequestResult {}
    
    enum ErrorType {
        TIMEOUT,
        CONNECTION_REFUSED,
        UNKNOWN
    }
}
```

##### LoadTester (Facade)

```java
public final class LoadTester {
    
    private LoadTester() {}
    
    public static TestResult run(LoadTestConfig config) {
        return run(config, progress -> {});
    }
    
    public static TestResult run(LoadTestConfig config, Consumer<Progress> progressCallback) {
        HttpClientPort httpClient = new JdkHttpClient(config.timeout());
        VirtualThreadEngine engine = new VirtualThreadEngine(httpClient);
        MetricAggregator aggregator = new MetricAggregator();
        
        List<RequestResult> results = engine.execute(config, progressCallback);
        return aggregator.aggregate(results);
    }
    
    public record Progress(int completed, int total) {
        public double percentage() {
            return total > 0 ? (double) completed / total * 100 : 0;
        }
    }
}
```

##### VirtualThreadEngine (Virtual Threads 구현)

```java
public class VirtualThreadEngine implements LoadTestEngine {
    
    private final HttpClientPort httpClient;
    
    public VirtualThreadEngine(HttpClientPort httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public List<RequestResult> execute(LoadTestConfig config, Consumer<Progress> progressCallback) {
        List<RequestResult> results = new CopyOnWriteArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        Semaphore semaphore = new Semaphore(config.concurrency());
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < config.totalRequests(); i++) {
                futures.add(executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            RequestResult result = httpClient.send(
                                config.url(),
                                config.method(),
                                config.headers(),
                                config.body()
                            );
                            results.add(result);
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    int current = completed.incrementAndGet();
                    progressCallback.accept(new Progress(current, config.totalRequests()));
                }));
            }
            
            // 모든 요청 완료 대기
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Test execution failed", e);
        }
        
        return results;
    }
}
```

##### JdkHttpClient (JDK HttpClient 어댑터)

```java
public class JdkHttpClient implements HttpClientPort {
    
    private final java.net.http.HttpClient client;
    private final Duration timeout;
    
    public JdkHttpClient(Duration timeout) {
        this.timeout = timeout;
        this.client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build();
    }
    
    @Override
    public RequestResult send(String url, HttpMethod method, 
                              Map<String, String> headers, String body) {
        long startTime = System.currentTimeMillis();
        
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout);
            
            // 헤더 추가
            headers.forEach(requestBuilder::header);
            
            // 메서드 및 바디 설정
            switch (method) {
                case GET -> requestBuilder.GET();
                case POST -> requestBuilder.POST(
                    body != null ? HttpRequest.BodyPublishers.ofString(body) 
                                 : HttpRequest.BodyPublishers.noBody()
                );
                case PUT -> requestBuilder.PUT(
                    body != null ? HttpRequest.BodyPublishers.ofString(body)
                                 : HttpRequest.BodyPublishers.noBody()
                );
                case DELETE -> requestBuilder.DELETE();
            }
            
            HttpResponse<Void> response = client.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.discarding()
            );
            
            long latency = System.currentTimeMillis() - startTime;
            return new RequestResult.Success(response.statusCode(), latency);
            
        } catch (HttpTimeoutException e) {
            return new RequestResult.Failure(e.getMessage(), ErrorType.TIMEOUT);
        } catch (ConnectException e) {
            return new RequestResult.Failure(e.getMessage(), ErrorType.CONNECTION_REFUSED);
        } catch (Exception e) {
            return new RequestResult.Failure(e.getMessage(), ErrorType.UNKNOWN);
        }
    }
}
```

---

### overload-cli 설계

#### 패키지 구조

```
overload-cli/
└── src/main/java/io/github/junhyeong9812/overload/cli/
    │
    ├── command/
    │   ├── RootCommand.java
    │   └── RunCommand.java
    │
    ├── output/
    │   ├── OutputFormatter.java
    │   ├── TextFormatter.java
    │   └── JsonFormatter.java
    │
    ├── progress/
    │   └── ProgressBar.java
    │
    └── Main.java
```

#### 핵심 클래스

##### Main (진입점)

```java
public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new RootCommand())
            .execute(args);
        System.exit(exitCode);
    }
}
```

##### RootCommand (루트 명령어)

```java
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
    
    @Override
    public void run() {
        System.out.println("Use 'overload run' to execute a load test.");
        System.out.println("Use 'overload --help' for more options.");
    }
}
```

##### RunCommand (run 명령어)

```java
@Command(
    name = "run",
    description = "Execute HTTP load test"
)
public class RunCommand implements Callable<Integer> {
    
    @Option(names = {"-u", "--url"}, required = true, description = "Target URL")
    private String url;
    
    @Option(names = {"-m", "--method"}, defaultValue = "GET", description = "HTTP method")
    private String method;
    
    @Option(names = {"-c", "--concurrency"}, defaultValue = "10", description = "Concurrent requests")
    private int concurrency;
    
    @Option(names = {"-n", "--requests"}, defaultValue = "100", description = "Total requests")
    private int totalRequests;
    
    @Option(names = {"-t", "--timeout"}, defaultValue = "5000", description = "Timeout in ms")
    private int timeout;
    
    @Option(names = {"-H", "--header"}, description = "HTTP headers (key:value)")
    private List<String> headers = new ArrayList<>();
    
    @Option(names = {"-d", "--data"}, description = "Request body")
    private String body;
    
    @Option(names = {"-o", "--output"}, defaultValue = "text", description = "Output format (text, json)")
    private String outputFormat;
    
    @Option(names = {"-q", "--quiet"}, description = "Minimal output")
    private boolean quiet;
    
    @Override
    public Integer call() {
        try {
            // 설정 생성
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
            
            if (body != null) {
                builder.body(body);
            }
            
            LoadTestConfig config = builder.build();
            
            // 헤더 출력
            if (!quiet) {
                printHeader(config);
            }
            
            // 프로그레스 바
            ProgressBar progressBar = new ProgressBar(totalRequests);
            
            // 테스트 실행
            TestResult result = LoadTester.run(config, progress -> {
                if (!quiet) {
                    progressBar.update(progress.completed());
                }
            });
            
            if (!quiet) {
                progressBar.complete();
            }
            
            // 결과 출력
            OutputFormatter formatter = "json".equals(outputFormat) 
                ? new JsonFormatter() 
                : new TextFormatter();
            System.out.println(formatter.format(result));
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
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
}
```

##### ProgressBar (진행률 표시)

```java
public class ProgressBar {
    
    private static final int WIDTH = 40;
    private final int total;
    
    public ProgressBar(int total) {
        this.total = total;
    }
    
    public void update(int current) {
        double percentage = (double) current / total;
        int filled = (int) (WIDTH * percentage);
        
        StringBuilder bar = new StringBuilder("\r[");
        for (int i = 0; i < WIDTH; i++) {
            if (i < filled) bar.append("█");
            else bar.append("░");
        }
        bar.append(String.format("] %3.0f%% (%,d/%,d)", percentage * 100, current, total));
        
        System.out.print(bar);
    }
    
    public void complete() {
        update(total);
        System.out.println();
        System.out.println();
    }
}
```

##### TextFormatter (텍스트 출력)

```java
public class TextFormatter implements OutputFormatter {
    
    @Override
    public String format(TestResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Results:\n");
        sb.append(String.format("  Total Requests:    %,d%n", result.totalRequests()));
        sb.append(String.format("  Successful:        %,d (%.1f%%)%n", 
            result.successCount(), result.successRate()));
        sb.append(String.format("  Failed:            %,d (%.1f%%)%n", 
            result.failCount(), 100 - result.successRate()));
        sb.append(String.format("  Total Time:        %.2fs%n", 
            result.totalDuration().toMillis() / 1000.0));
        sb.append(String.format("  Requests/sec:      %.2f%n", result.requestsPerSecond()));
        sb.append("\n");
        
        var latency = result.latencyStats();
        sb.append("Latency Distribution:\n");
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
}
```

---

### Phase 1 구현 체크리스트

#### overload-core

- [ ] 프로젝트 구조 생성
- [ ] config 패키지
    - [ ] LoadTestConfig
    - [ ] HttpMethod
- [ ] http 패키지
    - [ ] HttpClientPort 인터페이스
    - [ ] JdkHttpClient 구현
    - [ ] RequestResult (sealed interface)
- [ ] engine 패키지
    - [ ] LoadTestEngine 인터페이스
    - [ ] VirtualThreadEngine 구현
- [ ] metric 패키지
    - [ ] TestResult
    - [ ] Percentiles
    - [ ] MetricAggregator
- [ ] LoadTester (Facade)
- [ ] 단위 테스트

#### overload-cli

- [ ] 프로젝트 구조 생성
- [ ] command 패키지
    - [ ] RootCommand
    - [ ] RunCommand
- [ ] output 패키지
    - [ ] OutputFormatter 인터페이스
    - [ ] TextFormatter
    - [ ] JsonFormatter
- [ ] progress 패키지
    - [ ] ProgressBar
- [ ] Main 진입점
- [ ] Fat JAR 빌드 설정

---

## Phase 2: Advanced Features

### 목표

실제 사용에 필요한 고급 기능 추가

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| YAML 설정 | `-f scenario.yaml`로 설정 로드 | P0 |
| 시간 기반 테스트 | `-d 60s`로 N초 동안 테스트 | P0 |
| Ramp-up | 점진적 부하 증가 | P1 |
| 결과 내보내기 | `--output csv`, JSON/CSV 파일 저장 | P1 |
| 환경 변수 | YAML에서 `${VAR}` 치환 | P1 |
| 다중 URL | 시나리오에서 여러 URL 테스트 | P2 |
| 인증 지원 | Bearer Token, Basic Auth 편의 옵션 | P2 |

### YAML 설정 예시

```yaml
name: "User API Load Test"

target:
  url: https://api.example.com/users
  method: GET
  headers:
    Authorization: "Bearer ${TOKEN}"
    Content-Type: "application/json"

load:
  concurrency: 100
  requests: 10000
  # 또는
  # duration: 60s
  
  # Ramp-up (선택)
  rampUp:
    duration: 10s
    startConcurrency: 10

options:
  timeout: 5000
  
output:
  format: json
  file: results.json
```

### 추가 도메인 모델

#### LoadStrategy (Strategy Pattern)

```java
public sealed interface LoadStrategy 
    permits ConstantLoadStrategy, RampUpLoadStrategy {
    
    int getConcurrencyAt(Duration elapsed);
}

public record ConstantLoadStrategy(int concurrency) implements LoadStrategy {
    @Override
    public int getConcurrencyAt(Duration elapsed) {
        return concurrency;
    }
}

public record RampUpLoadStrategy(
    int startConcurrency,
    int targetConcurrency,
    Duration rampUpDuration
) implements LoadStrategy {
    @Override
    public int getConcurrencyAt(Duration elapsed) {
        if (elapsed.compareTo(rampUpDuration) >= 0) {
            return targetConcurrency;
        }
        double progress = (double) elapsed.toMillis() / rampUpDuration.toMillis();
        return (int) (startConcurrency + (targetConcurrency - startConcurrency) * progress);
    }
}
```

### Phase 2 구현 체크리스트

- [ ] YAML 설정 로더
    - [ ] YamlConfigLoader
    - [ ] 환경 변수 치환
- [ ] 시간 기반 테스트
    - [ ] DurationBasedEngine
- [ ] LoadStrategy 패턴
    - [ ] ConstantLoadStrategy
    - [ ] RampUpLoadStrategy
- [ ] 결과 내보내기
    - [ ] CsvFormatter
    - [ ] 파일 저장 옵션

---

## Phase 3: Web Dashboard

### 목표

선택적 웹 기반 대시보드 및 실시간 모니터링

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 대시보드 | 최근 테스트, 통계 요약 | P0 |
| 테스트 생성 | 웹 폼으로 테스트 설정 | P0 |
| 실시간 모니터링 | SSE로 진행 상황 표시 | P1 |
| 차트 | 응답시간 분포, TPS 추이 | P1 |
| 테스트 이력 | 과거 테스트 조회 | P2 |

### 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 4.0.0 |
| Template | Thymeleaf |
| Styling | Tailwind CSS |
| Interaction | HTMX |
| Charts | Chart.js |
| Realtime | SSE |

### CLI 연동

```bash
# 웹 서버 시작
overload serve --port 8080
```

```java
// ServeCommand
@Command(name = "serve", description = "Start web dashboard")
public class ServeCommand implements Callable<Integer> {
    
    @Option(names = {"-p", "--port"}, defaultValue = "8080")
    private int port;
    
    @Override
    public Integer call() {
        SpringApplication app = new SpringApplication(WebApplication.class);
        app.setDefaultProperties(Map.of("server.port", port));
        app.run();
        return 0;
    }
}
```

### Phase 3 구현 체크리스트

- [ ] overload-web 모듈 생성
- [ ] Spring Boot 설정
- [ ] 대시보드 페이지
- [ ] 테스트 생성 폼
- [ ] REST API
- [ ] SSE 실시간 업데이트
- [ ] CLI serve 명령어 연동

---

## 도메인 모델

### 클래스 다이어그램

```
┌─────────────────────┐
│   LoadTestConfig    │
├─────────────────────┤
│ - url               │
│ - method            │
│ - concurrency       │
│ - totalRequests     │
│ - timeout           │
│ - headers           │
│ - body              │
└─────────────────────┘
          │
          │ 입력
          ▼
┌─────────────────────┐
│   VirtualThread     │
│      Engine         │
├─────────────────────┤
│ + execute()         │
└─────────────────────┘
          │
          │ 생성
          ▼
┌─────────────────────┐
│   RequestResult     │
├─────────────────────┤
│ ├─ Success          │
│ │   - statusCode    │
│ │   - latencyMs     │
│ └─ Failure          │
│     - errorMessage  │
│     - errorType     │
└─────────────────────┘
          │
          │ 집계
          ▼
┌─────────────────────┐
│    TestResult       │
├─────────────────────┤
│ - totalRequests     │
│ - successCount      │
│ - failCount         │
│ - requestsPerSecond │
│ - latencyStats      │
│   └─ percentiles    │
└─────────────────────┘
```

---

## 기술적 고려사항

### Virtual Threads 사용

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Virtual Thread 생성
}
```

**장점:**
- 수만 개 동시 요청 가능
- 메모리 효율적 (~1KB/thread)
- 동기식 코드로 비동기 성능

**주의점:**
- synchronized 블록 내 I/O 피하기 (Java 24+에서 해결)
- Thread Pool 사용하지 않기

### 동시성 제어 (Semaphore)

```java
Semaphore semaphore = new Semaphore(concurrency);

semaphore.acquire();
try {
    // HTTP 요청
} finally {
    semaphore.release();
}
```

### Percentile 계산

```java
public Percentiles calculate(List<Long> latencies) {
    Collections.sort(latencies);
    int size = latencies.size();
    
    return new Percentiles(
        latencies.get((int) (size * 0.50)),
        latencies.get((int) (size * 0.90)),
        latencies.get((int) (size * 0.95)),
        latencies.get((int) (size * 0.99))
    );
}
```

---

## 마일스톤

| Phase | 기간 | 결과물 |
|-------|------|--------|
| Phase 1 | 2주 | 동작하는 CLI 도구 |
| Phase 2 | 2주 | YAML, Ramp-up, 내보내기 |
| Phase 3 | 2주 | 웹 대시보드 |

---

## 참고

- [프로젝트 README](../../README.md)
- [아키텍처 문서](../architecture/README.md)
- [picocli 문서](https://picocli.info/)
- [Virtual Threads - JEP 444](https://openjdk.org/jeps/444)