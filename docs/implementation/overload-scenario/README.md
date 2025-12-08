# overload-scenario 구현 계획

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | overload-scenario |
| Java 버전 | 21 |
| 의존성 | overload-core |
| 목적 | 다단계 시나리오 기반 부하 테스트 |

---

## 핵심 개념

### 시나리오 테스트란?

단순 부하 테스트가 **단일 API**에 대한 반복 호출이라면,  
시나리오 테스트는 **여러 API를 순차적으로 연결**하여 실제 사용자 흐름을 시뮬레이션합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    Scenario Iteration                       │
│                                                             │
│  ┌─────────┐     ┌─────────────┐     ┌───────────────┐     │
│  │  Login  │────▶│ Get Profile │────▶│ Update Profile│     │
│  │         │     │             │     │               │     │
│  │ extract │     │ use ${token}│     │ use ${token}  │     │
│  │  token  │     │             │     │               │     │
│  └─────────┘     └─────────────┘     └───────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
            ↓ (iterations × concurrency)
┌─────────────────────────────────────────────────────────────┐
│  동일 시나리오를 여러 가상 사용자가 동시에 반복 실행        │
└─────────────────────────────────────────────────────────────┘
```

### 주요 기능

| 기능 | 설명 |
|------|------|
| **변수 추출** | JSONPath로 응답에서 값 추출 (`$.data.token`) |
| **변수 치환** | 추출된 값을 다음 요청에 사용 (`${login.token}`) |
| **실패 전략** | STOP, SKIP, RETRY 지원 |
| **Step별 통계** | 각 Step의 성공률, 레이턴시 개별 측정 |

---

## 의존성 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    overload-starter                         │
│  - Web UI (Scenario Test 탭)                                │
│  - ScenarioApiController                                    │
│  - ScenarioTestService                                      │
└─────────────────────┬───────────────────────────────────────┘
                      │ depends on
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    overload-scenario                        │
│  - Scenario 도메인 정의                                     │
│  - 변수 추출/치환                                           │
│  - ScenarioExecutor                                         │
│  - ScenarioLoadTester (Facade)                              │
└─────────────────────┬───────────────────────────────────────┘
                      │ depends on
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                      overload-core                          │
│  - HttpClientPort                                           │
│  - RequestResult                                            │
│  - Virtual Thread Engine                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 폴더 구조

```
overload-scenario/
├── build.gradle
└── src/
    └── main/
        └── java/
            └── io/github/junhyeong9812/overload/scenario/
                │
                ├── ScenarioLoadTester.java           # Facade (진입점)
                │
                ├── scenario/                         # 시나리오 정의
                │   ├── domain/
                │   │   ├── Scenario.java             # 시나리오 전체
                │   │   ├── ScenarioStep.java         # 개별 Step
                │   │   ├── FailureStrategy.java      # 실패 전략
                │   │   ├── ScenarioResult.java       # 1회 실행 결과
                │   │   ├── StepResult.java           # Step 실행 결과
                │   │   ├── ScenarioTestResult.java   # 전체 테스트 결과
                │   │   └── StepStats.java            # Step별 통계
                │   │
                │   └── application/
                │       ├── service/
                │       │   └── ScenarioExecutor.java # 시나리오 실행
                │       └── callback/
                │           └── ScenarioProgressCallback.java
                │
                └── variable/                         # 변수 처리
                    ├── domain/
                    │   └── VariableContext.java      # 변수 저장소
                    └── application/
                        ├── VariableExtractor.java    # 응답에서 변수 추출
                        └── VariableResolver.java     # 변수 치환
```

---

## 구현 상세

### 1. scenario/domain 패키지

#### FailureStrategy.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

/**
 * Step 실패 시 처리 전략.
 */
public enum FailureStrategy {
    
    /** 실패 시 해당 시나리오 즉시 중단 */
    STOP,
    
    /** 실패한 Step을 건너뛰고 다음 Step 계속 진행 */
    SKIP,
    
    /** 실패 시 지정 횟수만큼 재시도 */
    RETRY
}
```

#### ScenarioStep.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

import io.github.junhyeong9812.overload.core.config.HttpMethod;
import java.util.Map;

/**
 * 시나리오의 개별 Step.
 *
 * @param id       Step 고유 ID (변수 참조용)
 * @param name     Step 이름 (표시용)
 * @param method   HTTP 메서드
 * @param url      요청 URL (변수 포함 가능)
 * @param headers  HTTP 헤더 (변수 포함 가능)
 * @param body     요청 본문 (변수 포함 가능)
 * @param extract  응답에서 추출할 변수 (변수명 → JSONPath)
 * @param timeoutMs 타임아웃 (ms)
 */
public record ScenarioStep(
    String id,
    String name,
    HttpMethod method,
    String url,
    Map<String, String> headers,
    String body,
    Map<String, String> extract,
    long timeoutMs
) {
    public ScenarioStep {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Step ID is required");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }
        if (method == null) {
            method = HttpMethod.GET;
        }
        if (headers == null) {
            headers = Map.of();
        }
        if (extract == null) {
            extract = Map.of();
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30000L;
        }
    }
}
```

#### Scenario.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.List;

/**
 * 시나리오 정의.
 *
 * @param name            시나리오 이름
 * @param steps           Step 목록 (순차 실행)
 * @param failureStrategy 실패 전략
 * @param retryCount      재시도 횟수 (RETRY 전략 시)
 * @param retryDelayMs    재시도 간격 (ms)
 */
public record Scenario(
    String name,
    List<ScenarioStep> steps,
    FailureStrategy failureStrategy,
    int retryCount,
    long retryDelayMs
) {
    public Scenario {
        if (name == null || name.isBlank()) {
            name = "Unnamed Scenario";
        }
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("At least one step is required");
        }
        if (failureStrategy == null) {
            failureStrategy = FailureStrategy.STOP;
        }
        if (retryCount < 0) {
            retryCount = 0;
        }
        if (retryDelayMs < 0) {
            retryDelayMs = 1000L;
        }
    }
}
```

#### StepResult.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.Map;

/**
 * 단일 Step 실행 결과.
 */
public record StepResult(
    String stepId,
    String stepName,
    boolean success,
    int statusCode,
    long latencyMs,
    String errorMessage,
    Map<String, String> extractedVariables
) {
    public static StepResult success(
            String stepId, String stepName, int statusCode, 
            long latencyMs, Map<String, String> extracted) {
        return new StepResult(stepId, stepName, true, statusCode, 
                              latencyMs, null, extracted);
    }
    
    public static StepResult failure(
            String stepId, String stepName, long latencyMs, String error) {
        return new StepResult(stepId, stepName, false, 0, 
                              latencyMs, error, Map.of());
    }
}
```

#### ScenarioResult.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.List;

/**
 * 단일 시나리오 1회 실행 결과.
 */
public record ScenarioResult(
    boolean success,
    long totalDurationMs,
    List<StepResult> stepResults
) {
    public static ScenarioResult success(long duration, List<StepResult> steps) {
        return new ScenarioResult(true, duration, steps);
    }
    
    public static ScenarioResult failure(long duration, List<StepResult> steps) {
        return new ScenarioResult(false, duration, steps);
    }
}
```

#### StepStats.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

/**
 * Step별 집계 통계.
 */
public record StepStats(
    String stepId,
    String stepName,
    int totalCount,
    int successCount,
    int failCount,
    long minLatencyMs,
    long maxLatencyMs,
    double avgLatencyMs
) {
    public double successRate() {
        return totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
    }
}
```

#### ScenarioTestResult.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.domain;

import java.util.Map;

/**
 * 시나리오 테스트 전체 결과.
 */
public record ScenarioTestResult(
    String scenarioName,
    int totalIterations,
    int successCount,
    int failCount,
    long totalDurationMs,
    double avgDurationMs,
    double scenariosPerSecond,
    Map<String, StepStats> stepStats
) {
    public double successRate() {
        return totalIterations > 0 
            ? (double) successCount / totalIterations * 100 
            : 0;
    }
}
```

---

### 2. variable 패키지

#### domain/VariableContext.java

```java
package io.github.junhyeong9812.overload.scenario.variable.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 시나리오 실행 중 변수를 저장하는 컨텍스트.
 * 
 * <p>각 시나리오 Iteration마다 새로운 컨텍스트가 생성된다.
 */
public class VariableContext {
    
    /** stepId.variableName → value */
    private final Map<String, String> variables = new HashMap<>();
    
    /**
     * 변수를 저장한다.
     *
     * @param stepId   Step ID
     * @param name     변수명
     * @param value    값
     */
    public void put(String stepId, String name, String value) {
        String key = stepId + "." + name;
        variables.put(key, value);
    }
    
    /**
     * 변수를 조회한다.
     *
     * @param stepId Step ID
     * @param name   변수명
     * @return 값 (없으면 empty)
     */
    public Optional<String> get(String stepId, String name) {
        String key = stepId + "." + name;
        return Optional.ofNullable(variables.get(key));
    }
    
    /**
     * 전체 변수 맵을 반환한다.
     */
    public Map<String, String> getAll() {
        return Map.copyOf(variables);
    }
    
    /**
     * 컨텍스트를 초기화한다.
     */
    public void clear() {
        variables.clear();
    }
}
```

#### application/VariableExtractor.java

```java
package io.github.junhyeong9812.overload.scenario.variable.application;

import io.github.junhyeong9812.overload.scenario.variable.domain.VariableContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 응답에서 변수를 추출한다.
 * 
 * <p>JSONPath를 지원한다: $.data.token
 */
public class VariableExtractor {
    
    /**
     * 응답 본문에서 변수를 추출하여 컨텍스트에 저장한다.
     *
     * @param responseBody 응답 본문 (JSON)
     * @param extractRules 추출 규칙 (변수명 → JSONPath)
     * @param stepId       Step ID
     * @param context      변수 컨텍스트
     * @return 추출된 변수 맵
     */
    public Map<String, String> extract(
            String responseBody,
            Map<String, String> extractRules,
            String stepId,
            VariableContext context) {
        
        Map<String, String> extracted = new java.util.HashMap<>();
        
        for (Map.Entry<String, String> rule : extractRules.entrySet()) {
            String varName = rule.getKey();
            String jsonPath = rule.getValue();
            
            String value = extractByJsonPath(responseBody, jsonPath);
            if (value != null) {
                context.put(stepId, varName, value);
                extracted.put(varName, value);
            }
        }
        
        return extracted;
    }
    
    /**
     * 간단한 JSONPath 추출.
     * 
     * <p>지원 형식: $.field, $.parent.child
     */
    private String extractByJsonPath(String json, String path) {
        if (json == null || path == null) return null;
        
        // $. 제거
        String fieldPath = path.startsWith("$.") ? path.substring(2) : path;
        String[] fields = fieldPath.split("\\.");
        
        String current = json;
        for (String field : fields) {
            current = extractField(current, field);
            if (current == null) return null;
        }
        
        return current;
    }
    
    /**
     * JSON에서 단일 필드 값을 추출한다.
     */
    private String extractField(String json, String field) {
        // "field": "value" 또는 "field": value 패턴
        Pattern pattern = Pattern.compile(
            "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"?([^,\"\\}]+)\"?"
        );
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
```

#### application/VariableResolver.java

```java
package io.github.junhyeong9812.overload.scenario.variable.application;

import io.github.junhyeong9812.overload.scenario.variable.domain.VariableContext;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문자열 내 변수 참조를 실제 값으로 치환한다.
 * 
 * <p>변수 참조 형식: ${stepId.variableName}
 */
public class VariableResolver {
    
    private static final Pattern VARIABLE_PATTERN = 
        Pattern.compile("\\$\\{([^.]+)\\.([^}]+)}");
    
    /**
     * 문자열 내 변수 참조를 치환한다.
     *
     * @param template 원본 문자열
     * @param context  변수 컨텍스트
     * @return 치환된 문자열
     */
    public String resolve(String template, VariableContext context) {
        if (template == null) return null;
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        
        while (matcher.find()) {
            String stepId = matcher.group(1);
            String varName = matcher.group(2);
            
            String value = context.get(stepId, varName).orElse("");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Map 내 모든 값의 변수 참조를 치환한다.
     *
     * @param map     원본 맵
     * @param context 변수 컨텍스트
     * @return 치환된 맵
     */
    public Map<String, String> resolveMap(
            Map<String, String> map, VariableContext context) {
        
        if (map == null) return Map.of();
        
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            resolved.put(entry.getKey(), resolve(entry.getValue(), context));
        }
        return resolved;
    }
}
```

---

### 3. scenario/application 패키지

#### callback/ScenarioProgressCallback.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.application.callback;

import io.github.junhyeong9812.overload.scenario.scenario.domain.ScenarioResult;

/**
 * 시나리오 테스트 진행 상황 콜백.
 */
@FunctionalInterface
public interface ScenarioProgressCallback {
    
    /**
     * 시나리오 1회 완료 시 호출된다.
     *
     * @param completed 완료된 시나리오 수
     * @param total     전체 시나리오 수
     * @param result    실행 결과
     */
    void onProgress(int completed, int total, ScenarioResult result);
    
    static ScenarioProgressCallback noop() {
        return (completed, total, result) -> {};
    }
}
```

#### service/ScenarioExecutor.java

```java
package io.github.junhyeong9812.overload.scenario.scenario.application.service;

import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.domain.HttpRequest;
import io.github.junhyeong9812.overload.core.http.domain.RequestResult;
import io.github.junhyeong9812.overload.scenario.scenario.domain.*;
import io.github.junhyeong9812.overload.scenario.variable.application.VariableExtractor;
import io.github.junhyeong9812.overload.scenario.variable.application.VariableResolver;
import io.github.junhyeong9812.overload.scenario.variable.domain.VariableContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 1회 실행을 담당한다.
 */
public class ScenarioExecutor {
    
    private final HttpClientPort httpClient;
    private final VariableExtractor extractor;
    private final VariableResolver resolver;
    
    public ScenarioExecutor(HttpClientPort httpClient) {
        this.httpClient = httpClient;
        this.extractor = new VariableExtractor();
        this.resolver = new VariableResolver();
    }
    
    /**
     * 시나리오를 1회 실행한다.
     *
     * @param scenario 시나리오
     * @return 실행 결과
     */
    public ScenarioResult execute(Scenario scenario) {
        VariableContext context = new VariableContext();
        List<StepResult> stepResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        boolean scenarioSuccess = true;
        
        for (ScenarioStep step : scenario.steps()) {
            StepResult result = executeStep(step, context, scenario);
            stepResults.add(result);
            
            if (!result.success()) {
                scenarioSuccess = false;
                
                if (scenario.failureStrategy() == FailureStrategy.STOP) {
                    break;
                }
                // SKIP: 계속 진행
                // RETRY: executeStep 내부에서 처리
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        return scenarioSuccess 
            ? ScenarioResult.success(duration, stepResults)
            : ScenarioResult.failure(duration, stepResults);
    }
    
    /**
     * 단일 Step을 실행한다 (재시도 포함).
     */
    private StepResult executeStep(
            ScenarioStep step, 
            VariableContext context,
            Scenario scenario) {
        
        int maxAttempts = scenario.failureStrategy() == FailureStrategy.RETRY 
            ? scenario.retryCount() + 1 
            : 1;
        
        StepResult lastResult = null;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                sleep(scenario.retryDelayMs());
            }
            
            lastResult = executeStepOnce(step, context);
            
            if (lastResult.success()) {
                return lastResult;
            }
        }
        
        return lastResult;
    }
    
    /**
     * Step을 1회 실행한다.
     */
    private StepResult executeStepOnce(ScenarioStep step, VariableContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 변수 치환
            String resolvedUrl = resolver.resolve(step.url(), context);
            Map<String, String> resolvedHeaders = resolver.resolveMap(step.headers(), context);
            String resolvedBody = resolver.resolve(step.body(), context);
            
            // HTTP 요청
            HttpRequest request = HttpRequest.from(
                resolvedUrl, step.method(), resolvedHeaders, resolvedBody
            );
            
            RequestResult result = httpClient.send(request);
            long latency = System.currentTimeMillis() - startTime;
            
            if (result instanceof RequestResult.Success success) {
                // 변수 추출 (응답 본문 필요 시 별도 처리)
                Map<String, String> extracted = Map.of();
                
                return StepResult.success(
                    step.id(), step.name(), success.statusCode(), latency, extracted
                );
            } else if (result instanceof RequestResult.Failure failure) {
                return StepResult.failure(
                    step.id(), step.name(), latency, failure.errorMessage()
                );
            }
            
            return StepResult.failure(step.id(), step.name(), latency, "Unknown error");
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return StepResult.failure(step.id(), step.name(), latency, e.getMessage());
        }
    }
    
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

### 4. ScenarioLoadTester.java (Facade)

```java
package io.github.junhyeong9812.overload.scenario;

import io.github.junhyeong9812.overload.core.http.application.port.HttpClientPort;
import io.github.junhyeong9812.overload.core.http.infrastructure.JdkHttpClient;
import io.github.junhyeong9812.overload.scenario.scenario.application.callback.ScenarioProgressCallback;
import io.github.junhyeong9812.overload.scenario.scenario.application.service.ScenarioExecutor;
import io.github.junhyeong9812.overload.scenario.scenario.domain.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 시나리오 부하 테스트 실행 Facade.
 * 
 * 사용 예시:
 * <pre>
 * Scenario scenario = new Scenario("Login Flow", steps, FailureStrategy.STOP, 0, 0);
 * 
 * ScenarioTestResult result = ScenarioLoadTester.run(
 *     scenario,
 *     1000,                    // iterations
 *     50,                      // concurrency
 *     Duration.ofSeconds(30)   // timeout
 * );
 * </pre>
 */
public final class ScenarioLoadTester {
    
    private ScenarioLoadTester() {}
    
    public static ScenarioTestResult run(
            Scenario scenario,
            int iterations,
            int concurrency,
            Duration timeout) {
        return run(scenario, iterations, concurrency, timeout, 
                   ScenarioProgressCallback.noop());
    }
    
    public static ScenarioTestResult run(
            Scenario scenario,
            int iterations,
            int concurrency,
            Duration timeout,
            ScenarioProgressCallback callback) {
        
        HttpClientPort httpClient = new JdkHttpClient(timeout);
        ScenarioExecutor executor = new ScenarioExecutor(httpClient);
        
        List<ScenarioResult> results = new CopyOnWriteArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        Semaphore semaphore = new Semaphore(concurrency);
        
        long startTime = System.currentTimeMillis();
        
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < iterations; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        semaphore.acquire();
                        try {
                            ScenarioResult result = executor.execute(scenario);
                            results.add(result);
                            
                            int count = completed.incrementAndGet();
                            callback.onProgress(count, iterations, result);
                        } finally {
                            semaphore.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    // 개별 실패 무시
                }
            }
        }
        
        long totalDuration = System.currentTimeMillis() - startTime;
        
        return aggregateResults(scenario, results, totalDuration);
    }
    
    /**
     * 전체 결과를 집계한다.
     */
    private static ScenarioTestResult aggregateResults(
            Scenario scenario,
            List<ScenarioResult> results,
            long totalDurationMs) {
        
        int total = results.size();
        int successCount = (int) results.stream().filter(ScenarioResult::success).count();
        int failCount = total - successCount;
        
        double avgDuration = results.stream()
            .mapToLong(ScenarioResult::totalDurationMs)
            .average()
            .orElse(0);
        
        double scenariosPerSecond = totalDurationMs > 0 
            ? (double) total / totalDurationMs * 1000 
            : 0;
        
        // Step별 통계 집계
        Map<String, StepStats> stepStats = aggregateStepStats(scenario, results);
        
        return new ScenarioTestResult(
            scenario.name(),
            total,
            successCount,
            failCount,
            totalDurationMs,
            avgDuration,
            scenariosPerSecond,
            stepStats
        );
    }
    
    /**
     * Step별 통계를 집계한다.
     */
    private static Map<String, StepStats> aggregateStepStats(
            Scenario scenario,
            List<ScenarioResult> results) {
        
        Map<String, StepStats> stats = new LinkedHashMap<>();
        
        for (ScenarioStep step : scenario.steps()) {
            List<StepResult> stepResults = results.stream()
                .flatMap(r -> r.stepResults().stream())
                .filter(sr -> sr.stepId().equals(step.id()))
                .toList();
            
            if (stepResults.isEmpty()) continue;
            
            int totalCount = stepResults.size();
            int successCount = (int) stepResults.stream().filter(StepResult::success).count();
            int failCount = totalCount - successCount;
            
            LongSummaryStatistics latencyStats = stepResults.stream()
                .mapToLong(StepResult::latencyMs)
                .summaryStatistics();
            
            stats.put(step.id(), new StepStats(
                step.id(),
                step.name(),
                totalCount,
                successCount,
                failCount,
                latencyStats.getMin(),
                latencyStats.getMax(),
                latencyStats.getAverage()
            ));
        }
        
        return stats;
    }
}
```

---

## build.gradle

```groovy
plugins {
    id 'java-library'
}

description = 'Overload Scenario - Multi-step scenario load testing'

dependencies {
    // Core 모듈 의존
    api project(':overload-core')
    
    // 테스트
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}
```

---

## 테스트 계획

### 단위 테스트

| 대상 | 테스트 항목 |
|------|------------|
| `VariableContext` | 변수 저장/조회, 키 형식 |
| `VariableExtractor` | JSONPath 추출, 중첩 필드, null 처리 |
| `VariableResolver` | 변수 치환, 다중 변수, 미존재 변수 |
| `ScenarioExecutor` | Step 순차 실행, 실패 전략별 동작 |
| `ScenarioLoadTester` | 동시성 제어, 결과 집계 |

### 통합 테스트

| 시나리오 | 검증 항목 |
|----------|----------|
| 로그인 → 프로필 조회 | 변수 추출 및 치환 |
| STOP 전략 | 첫 실패 시 중단 |
| SKIP 전략 | 실패 건너뛰고 계속 |
| RETRY 전략 | 재시도 횟수, 딜레이 |

---

## 구현 체크리스트

- [ ] scenario/domain
    - [ ] FailureStrategy
    - [ ] ScenarioStep
    - [ ] Scenario
    - [ ] StepResult
    - [ ] ScenarioResult
    - [ ] StepStats
    - [ ] ScenarioTestResult
- [ ] variable/domain
    - [ ] VariableContext
- [ ] variable/application
    - [ ] VariableExtractor
    - [ ] VariableResolver
- [ ] scenario/application
    - [ ] ScenarioProgressCallback
    - [ ] ScenarioExecutor
- [ ] ScenarioLoadTester (Facade)
- [ ] 단위 테스트
- [ ] 통합 테스트

---

## 참고

- [overload-core 구현 계획](./overload-core.md)
- [overload-starter 구현 계획](./overload-starter.md)