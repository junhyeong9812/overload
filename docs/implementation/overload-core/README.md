# overload-core 구현 계획

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | overload-core |
| Java 버전 | 21 |
| 외부 의존성 | 없음 (순수 JDK) |
| 아키텍처 | Hexagonal Architecture |
| 목적 | Virtual Threads 기반 부하 테스트 엔진 |

---

## 폴더 구조

```
overload-core/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── io/github/junhyeong9812/overload/core/
    │   │       │
    │   │       ├── LoadTester.java                 # Facade (진입점)
    │   │       │
    │   │       ├── config/                         # 설정 모델
    │   │       │   ├── HttpMethod.java
    │   │       │   └── LoadTestConfig.java
    │   │       │
    │   │       ├── engine/                         # 테스트 실행 엔진
    │   │       │   ├── domain/
    │   │       │   │   ├── LoadTestEngine.java
    │   │       │   │   └── ExecutionContext.java
    │   │       │   ├── application/
    │   │       │   │   └── TestExecutor.java
    │   │       │   └── infrastructure/
    │   │       │       └── VirtualThreadEngine.java
    │   │       │
    │   │       ├── http/                           # HTTP 클라이언트
    │   │       │   ├── domain/
    │   │       │   │   ├── HttpRequest.java
    │   │       │   │   ├── HttpResponse.java
    │   │       │   │   └── RequestResult.java
    │   │       │   ├── application/
    │   │       │   │   └── port/
    │   │       │   │       └── HttpClientPort.java
    │   │       │   └── infrastructure/
    │   │       │       └── JdkHttpClient.java
    │   │       │
    │   │       ├── metric/                         # 메트릭 수집/계산
    │   │       │   ├── domain/
    │   │       │   │   ├── Percentiles.java
    │   │       │   │   ├── LatencyHistogram.java
    │   │       │   │   └── TestResult.java
    │   │       │   └── application/
    │   │       │       └── MetricAggregator.java
    │   │       │
    │   │       ├── callback/                       # 콜백 인터페이스
    │   │       │   └── ProgressCallback.java
    │   │       │
    │   │       └── exception/                      # 예외
    │   │           ├── LoadTestException.java
    │   │           └── HttpExecutionException.java
    │   │
    │   └── resources/
    │       └── META-INF/
    │
    └── test/
        └── java/
            └── io/github/junhyeong9812/overload/core/
                ├── engine/
                ├── http/
                └── metric/
```

---

## Hexagonal Architecture 적용

### 의존성 방향

```
infrastructure ──────▶ application ──────▶ domain
     │                      │                 │
   Adapter              Port/Service      Pure Java
   (구현체)              (인터페이스)       (POJO)
```

### 핵심 원칙

1. **Domain은 아무것도 의존하지 않음** - 순수 Java 코드
2. **의존성은 항상 안쪽을 향함** - infrastructure → application → domain
3. **Port는 인터페이스, Adapter는 구현체**

### HTTP 클라이언트 추상화 예시

```
┌─────────────────────────────────────────────────────────┐
│                         http                            │
│                                                         │
│  ┌─────────────────┐                                   │
│  │ infrastructure  │                                   │
│  │                 │                                   │
│  │ JdkHttpClient   │──────────┐                        │
│  │ (OkHttpClient)  │          │                        │
│  └─────────────────┘          │                        │
│                               ▼                        │
│                    ┌─────────────────┐                 │
│                    │   application   │                 │
│                    │                 │                 │
│                    │ HttpClientPort  │ (interface)     │
│                    └────────┬────────┘                 │
│                             │                          │
│                             ▼                          │
│                    ┌─────────────────┐                 │
│                    │     domain      │                 │
│                    │                 │                 │
│                    │ HttpRequest     │                 │
│                    │ HttpResponse    │                 │
│                    │ RequestResult   │                 │
│                    └─────────────────┘                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 구현 상세

### 1. config 패키지

#### HttpMethod.java

```java
package io.github.junhyeong9812.overload.core.config;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}
```

#### LoadTestConfig.java

```java
package io.github.junhyeong9812.overload.core.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record LoadTestConfig(
    String url,
    HttpMethod method,
    Map<String, String> headers,
    String body,
    int concurrency,
    int totalRequests,
    Duration timeout
) {
    public LoadTestConfig {
        Objects.requireNonNull(url, "URL is required");
        if (concurrency < 1) {
            throw new IllegalArgumentException("Concurrency must be >= 1");
        }
        if (totalRequests < 1) {
            throw new IllegalArgumentException("Total requests must be >= 1");
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String url;
        private HttpMethod method = HttpMethod.GET;
        private Map<String, String> headers = new HashMap<>();
        private String body;
        private int concurrency = 10;
        private int totalRequests = 100;
        private Duration timeout = Duration.ofSeconds(5);
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }
        
        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        
        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }
        
        public Builder totalRequests(int totalRequests) {
            this.totalRequests = totalRequests;
            return this;
        }
        
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public LoadTestConfig build() {
            return new LoadTestConfig(
                url, method, Map.copyOf(headers), body,
                concurrency, totalRequests, timeout
            );
        }
    }
}
```

---

### 2. http 패키지

#### domain/HttpRequest.java

```java
package io.github.junhyeong9812.overload.core.http.domain;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import java.util.Map;

public record HttpRequest(
    String url,
    HttpMethod method,
    Map<String, String> headers,
    String body
) {
    public static HttpRequest from(
            String url, HttpMethod method, 
            Map<String, String> headers, String body) {
        return new HttpRequest(url, method, Map.copyOf(headers), body);
    }
}
```

#### domain/HttpResponse.java

```java
package io.github.junhyeong9812.overload.core.http.domain;

import java.util.Map;

public record HttpResponse(
    int statusCode,
    Map<String, String> headers,
    String body
) {
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public boolean isServerError() {
        return statusCode >= 500;
    }
}
```

#### domain/RequestResult.java

```java
package io.github.junhyeong9812.overload.core.http.domain;

public sealed interface RequestResult 
    permits RequestResult.Success, RequestResult.Failure {
    
    long latencyMs();
    
    record Success(
        int statusCode,
        long latencyMs
    ) implements RequestResult {
        public boolean isHttpSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
    
    record Failure(
        String errorMessage,
        ErrorType errorType,
        long latencyMs
    ) implements RequestResult {}
    
    enum ErrorType {
        TIMEOUT,
        CONNECTION_REFUSED,
        CONNECTION_RESET,
        UNKNOWN
    }
}
```

#### application/port/HttpClientPort.java

```java
package io.github.junhyeong9812.overload.core.http.application.port;

import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

/**
 * HTTP 클라이언트 추상화 (Output Port)
 * 
 * 다른 HTTP 클라이언트로 교체 가능:
 * - JdkHttpClient (기본)
 * - OkHttpClient
 * - Apache HttpClient
 */
public interface HttpClientPort {
    
    RequestResult send(HttpRequest request);
}
```

#### infrastructure/JdkHttpClient.java

```java
package io.github.junhyeong9812.overload.core.http.infrastructure;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult.ErrorType;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class JdkHttpClient implements HttpClientPort {
    
    private final HttpClient client;
    private final Duration timeout;
    
    public JdkHttpClient(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build();
    }
    
    @Override
    public RequestResult send(HttpRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            java.net.http.HttpRequest.Builder builder = 
                java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(request.url()))
                    .timeout(timeout);
            
            // 헤더 추가
            request.headers().forEach(builder::header);
            
            // 메서드 및 바디 설정
            var bodyPublisher = request.body() != null
                ? java.net.http.HttpRequest.BodyPublishers.ofString(request.body())
                : java.net.http.HttpRequest.BodyPublishers.noBody();
            
            switch (request.method()) {
                case GET -> builder.GET();
                case POST -> builder.POST(bodyPublisher);
                case PUT -> builder.PUT(bodyPublisher);
                case DELETE -> builder.DELETE();
                case PATCH -> builder.method("PATCH", bodyPublisher);
                case HEAD -> builder.method("HEAD", bodyPublisher);
                case OPTIONS -> builder.method("OPTIONS", bodyPublisher);
            }
            
            HttpResponse<Void> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.discarding()
            );
            
            long latency = System.currentTimeMillis() - startTime;
            return new RequestResult.Success(response.statusCode(), latency);
            
        } catch (HttpTimeoutException e) {
            long latency = System.currentTimeMillis() - startTime;
            return new RequestResult.Failure(e.getMessage(), ErrorType.TIMEOUT, latency);
            
        } catch (ConnectException e) {
            long latency = System.currentTimeMillis() - startTime;
            return new RequestResult.Failure(e.getMessage(), ErrorType.CONNECTION_REFUSED, latency);
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new RequestResult.Failure(e.getMessage(), ErrorType.UNKNOWN, latency);
        }
    }
}
```

---

### 3. metric 패키지

#### domain/Percentiles.java

```java
package io.github.junhyeong9812.overload.core.metric.domain;

public record Percentiles(
    long p50,
    long p90,
    long p95,
    long p99,
    long min,
    long max
) {
    public static Percentiles empty() {
        return new Percentiles(0, 0, 0, 0, 0, 0);
    }
}
```

#### domain/TestResult.java

```java
package io.github.junhyeong9812.overload.core.metric.domain;

import java.time.Duration;

public record TestResult(
    int totalRequests,
    int successCount,
    int failCount,
    Duration totalDuration,
    double requestsPerSecond,
    LatencyStats latencyStats
) {
    public double successRate() {
        return totalRequests > 0 
            ? (double) successCount / totalRequests * 100 
            : 0;
    }
    
    public double failRate() {
        return 100 - successRate();
    }
    
    public record LatencyStats(
        long min,
        long max,
        double avg,
        Percentiles percentiles
    ) {
        public static LatencyStats empty() {
            return new LatencyStats(0, 0, 0, Percentiles.empty());
        }
    }
}
```

#### application/MetricAggregator.java

```java
package io.github.junhyeong9812.overload.core.metric.application;

import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.metric.domain.Percentiles;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult.LatencyStats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;

public class MetricAggregator {
    
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failCount = new LongAdder();
    private final List<Long> latencies = new CopyOnWriteArrayList<>();
    
    private long startTime;
    private long endTime;
    
    public void start() {
        this.startTime = System.currentTimeMillis();
    }
    
    public void end() {
        this.endTime = System.currentTimeMillis();
    }
    
    public void record(RequestResult result) {
        totalRequests.increment();
        latencies.add(result.latencyMs());
        
        if (result instanceof RequestResult.Success success) {
            if (success.isHttpSuccess()) {
                successCount.increment();
            } else {
                failCount.increment();
            }
        } else {
            failCount.increment();
        }
    }
    
    public TestResult aggregate() {
        int total = totalRequests.intValue();
        int success = successCount.intValue();
        int fail = failCount.intValue();
        
        Duration duration = Duration.ofMillis(endTime - startTime);
        double rps = duration.toMillis() > 0 
            ? (double) total / duration.toMillis() * 1000 
            : 0;
        
        LatencyStats latencyStats = calculateLatencyStats();
        
        return new TestResult(total, success, fail, duration, rps, latencyStats);
    }
    
    private LatencyStats calculateLatencyStats() {
        if (latencies.isEmpty()) {
            return LatencyStats.empty();
        }
        
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        
        long min = sorted.getFirst();
        long max = sorted.getLast();
        double avg = sorted.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        Percentiles percentiles = calculatePercentiles(sorted);
        
        return new LatencyStats(min, max, avg, percentiles);
    }
    
    private Percentiles calculatePercentiles(List<Long> sorted) {
        int size = sorted.size();
        
        return new Percentiles(
            sorted.get(percentileIndex(size, 0.50)),
            sorted.get(percentileIndex(size, 0.90)),
            sorted.get(percentileIndex(size, 0.95)),
            sorted.get(percentileIndex(size, 0.99)),
            sorted.getFirst(),
            sorted.getLast()
        );
    }
    
    private int percentileIndex(int size, double percentile) {
        int index = (int) Math.ceil(size * percentile) - 1;
        return Math.max(0, Math.min(index, size - 1));
    }
}
```

---

### 4. engine 패키지

#### domain/LoadTestEngine.java

```java
package io.github.junhyeong9812.overload.core.engine.domain;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

import java.util.List;

public interface LoadTestEngine {
    
    List<RequestResult> execute(LoadTestConfig config, ProgressCallback callback);
}
```

#### domain/ExecutionContext.java

```java
package io.github.junhyeong9812.overload.core.engine.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionContext {
    
    private final int totalRequests;
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    public ExecutionContext(int totalRequests) {
        this.totalRequests = totalRequests;
    }
    
    public int getTotalRequests() {
        return totalRequests;
    }
    
    public int getCompletedCount() {
        return completedCount.get();
    }
    
    public int incrementAndGetCompleted() {
        return completedCount.incrementAndGet();
    }
    
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    public void cancel() {
        cancelled.set(true);
    }
    
    public double getProgress() {
        return totalRequests > 0 
            ? (double) completedCount.get() / totalRequests * 100 
            : 0;
    }
}
```

#### infrastructure/VirtualThreadEngine.java

```java
package io.github.junhyeong9812.overload.core.engine.infrastructure;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.engine.domain.ExecutionContext;
import io.github.junhyeong9812.overload.core.engine.domain.LoadTestEngine;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class VirtualThreadEngine implements LoadTestEngine {
    
    private final HttpClientPort httpClient;
    
    public VirtualThreadEngine(HttpClientPort httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public List<RequestResult> execute(LoadTestConfig config, ProgressCallback callback) {
        List<RequestResult> results = new CopyOnWriteArrayList<>();
        ExecutionContext context = new ExecutionContext(config.totalRequests());
        Semaphore semaphore = new Semaphore(config.concurrency());
        
        HttpRequest request = HttpRequest.from(
            config.url(),
            config.method(),
            config.headers(),
            config.body()
        );
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < config.totalRequests(); i++) {
                futures.add(executor.submit(() -> {
                    if (context.isCancelled()) {
                        return;
                    }
                    
                    try {
                        semaphore.acquire();
                        try {
                            RequestResult result = httpClient.send(request);
                            results.add(result);
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    int completed = context.incrementAndGetCompleted();
                    callback.onProgress(completed, config.totalRequests());
                }));
            }
            
            // 모든 요청 완료 대기
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    // 개별 실패는 무시 (이미 results에 기록됨)
                }
            }
        }
        
        return results;
    }
}
```

---

### 5. callback 패키지

#### ProgressCallback.java

```java
package io.github.junhyeong9812.overload.core.callback;

@FunctionalInterface
public interface ProgressCallback {
    
    void onProgress(int completed, int total);
    
    default double getPercentage(int completed, int total) {
        return total > 0 ? (double) completed / total * 100 : 0;
    }
    
    static ProgressCallback noop() {
        return (completed, total) -> {};
    }
}
```

---

### 6. exception 패키지

#### LoadTestException.java

```java
package io.github.junhyeong9812.overload.core.exception;

public class LoadTestException extends RuntimeException {
    
    public LoadTestException(String message) {
        super(message);
    }
    
    public LoadTestException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

### 7. LoadTester.java (Facade)

```java
package io.github.junhyeong9812.overload.core;

import io.github.junhyeong9812.overload.core.callback.ProgressCallback;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.engine.infrastructure.VirtualThreadEngine;
import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.core.http.infrastructure.JdkHttpClient;
import io.github.junhyeong9812.overload.core.metric.application.MetricAggregator;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;

import java.util.List;

/**
 * 부하 테스트 실행 Facade
 * 
 * 사용 예시:
 * <pre>
 * LoadTestConfig config = LoadTestConfig.builder()
 *     .url("https://api.example.com")
 *     .concurrency(100)
 *     .totalRequests(10000)
 *     .build();
 * 
 * TestResult result = LoadTester.run(config);
 * </pre>
 */
public final class LoadTester {
    
    private LoadTester() {
        // Utility class
    }
    
    public static TestResult run(LoadTestConfig config) {
        return run(config, ProgressCallback.noop());
    }
    
    public static TestResult run(LoadTestConfig config, ProgressCallback callback) {
        HttpClientPort httpClient = new JdkHttpClient(config.timeout());
        return run(config, callback, httpClient);
    }
    
    public static TestResult run(
            LoadTestConfig config, 
            ProgressCallback callback,
            HttpClientPort httpClient) {
        
        VirtualThreadEngine engine = new VirtualThreadEngine(httpClient);
        MetricAggregator aggregator = new MetricAggregator();
        
        aggregator.start();
        List<RequestResult> results = engine.execute(config, callback);
        aggregator.end();
        
        for (RequestResult result : results) {
            aggregator.record(result);
        }
        
        return aggregator.aggregate();
    }
}
```

---

## 테스트 계획

### 단위 테스트

| 대상 | 테스트 항목 |
|------|------------|
| LoadTestConfig | Builder 검증, 필수값 검증, 기본값 |
| RequestResult | sealed interface 동작 |
| MetricAggregator | Percentile 계산, 통계 집계 |
| JdkHttpClient | HTTP 요청/응답, 타임아웃, 에러 처리 |
| VirtualThreadEngine | 동시성 제어, 진행률 콜백 |

### 통합 테스트

| 시나리오 | 검증 항목 |
|----------|----------|
| 정상 요청 | 성공률 100% |
| 타임아웃 | Failure로 기록 |
| 서버 에러 (5xx) | Success지만 HTTP 실패 |
| 동시성 제어 | Semaphore 정상 동작 |

---

## 구현 체크리스트

- [ ] config 패키지
    - [ ] HttpMethod
    - [ ] LoadTestConfig
- [ ] http 패키지
    - [ ] HttpRequest
    - [ ] HttpResponse
    - [ ] RequestResult
    - [ ] HttpClientPort
    - [ ] JdkHttpClient
- [ ] metric 패키지
    - [ ] Percentiles
    - [ ] TestResult
    - [ ] MetricAggregator
- [ ] engine 패키지
    - [ ] LoadTestEngine
    - [ ] ExecutionContext
    - [ ] VirtualThreadEngine
- [ ] callback 패키지
    - [ ] ProgressCallback
- [ ] exception 패키지
    - [ ] LoadTestException
- [ ] LoadTester (Facade)
- [ ] 단위 테스트
- [ ] 통합 테스트