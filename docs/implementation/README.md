# 구현 계획

이 문서는 Overload 프로젝트의 단계별 구현 계획과 상세 설계를 담고 있습니다.

---

## 목차

1. [개발 환경](#개발-환경)
2. [Phase 1: Core MVP](#phase-1-core-mvp)
3. [Phase 2: Advanced Features](#phase-2-advanced-features)
4. [Phase 3: UI & Monitoring](#phase-3-ui--monitoring)
5. [API 설계](#api-설계)
6. [도메인 모델](#도메인-모델)
7. [기술적 고려사항](#기술적-고려사항)

---

## 개발 환경

### 기술 스택

| 항목 | 버전 | 비고 |
|------|------|------|
| Java | 25 | Virtual Threads, Structured Concurrency |
| Spring Boot | 4.0.0 | Spring Framework 7 기반 |
| Gradle | 8.x | Groovy DSL |
| HTTP Client | JDK HttpClient | 내장 클라이언트 |

### build.gradle

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'io.github.junhyeong9812'
version = '0.0.1-SNAPSHOT'
description = 'High-performance HTTP load testing tool powered by Java Virtual Threads'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### application.yml

```yaml
spring:
  application:
    name: overload
  threads:
    virtual:
      enabled: true

server:
  port: 8080
  tomcat:
    threads:
      max: 200

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: always
```

---

## Phase 1: Core MVP

### 목표

최소 기능을 갖춘 동작하는 부하 테스트 도구 완성

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 단일 URL 테스트 | 지정한 URL로 GET/POST 요청 | P0 |
| 동시 사용자 설정 | Virtual Threads로 N개 동시 요청 | P0 |
| 요청 수 설정 | 총 요청 수 기반 테스트 | P0 |
| 결과 리포트 | 성공/실패, 평균 응답시간, TPS | P0 |
| 테스트 상태 조회 | 실행 중/완료 상태 확인 | P0 |

### 패키지 구조

```
io.github.junhyeong9812.overload/
│
├── loadtest/
│   ├── domain/
│   │   ├── LoadTest.java
│   │   ├── LoadTestId.java
│   │   ├── TestConfig.java
│   │   ├── TestResult.java
│   │   └── TestStatus.java
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   ├── ExecuteLoadTestUseCase.java
│   │   │   │   ├── GetTestResultUseCase.java
│   │   │   │   └── GetTestStatusUseCase.java
│   │   │   └── out/
│   │   │       └── LoadTestRepositoryPort.java
│   │   └── service/
│   │       └── LoadTestService.java
│   └── infrastructure/
│       ├── web/
│       │   ├── LoadTestController.java
│       │   └── dto/
│       │       ├── CreateLoadTestRequest.java
│       │       ├── LoadTestResponse.java
│       │       └── TestResultResponse.java
│       └── persistence/
│           └── InMemoryLoadTestRepository.java
│
├── engine/
│   ├── domain/
│   │   ├── TestEngine.java
│   │   ├── ExecutionContext.java
│   │   └── vo/
│   │       ├── RequestResult.java
│   │       ├── HttpMethod.java
│   │       └── RequestSpec.java
│   ├── application/
│   │   ├── port/
│   │   │   └── out/
│   │   │       └── HttpRequestPort.java
│   │   └── service/
│   │       └── TestExecutionService.java
│   └── infrastructure/
│       ├── executor/
│       │   └── VirtualThreadTestEngine.java
│       └── http/
│           └── JavaHttpClientAdapter.java
│
├── shared/
│   ├── util/
│   │   └── TimeUtils.java
│   └── exception/
│       ├── OverloadException.java
│       └── TestNotFoundException.java
│
└── config/
    └── BeanConfig.java
```

### 핵심 클래스 설계

#### LoadTest (Domain)

```java
public class LoadTest {
    private final LoadTestId id;
    private final String name;
    private final TestConfig config;
    private TestStatus status;
    private TestResult result;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    public static LoadTest create(String name, TestConfig config) {
        return new LoadTest(
            LoadTestId.generate(),
            name,
            config,
            TestStatus.READY,
            null,
            Instant.now(),
            null,
            null
        );
    }

    public void start() {
        validateCanStart();
        this.status = TestStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void complete(TestResult result) {
        this.result = result;
        this.status = TestStatus.COMPLETED;
        this.finishedAt = Instant.now();
    }

    public void fail(String reason) {
        this.status = TestStatus.FAILED;
        this.finishedAt = Instant.now();
    }

    private void validateCanStart() {
        if (status != TestStatus.READY) {
            throw new IllegalStateException("Test already started");
        }
    }
}
```

#### TestConfig (Domain VO)

```java
public record TestConfig(
    String targetUrl,
    HttpMethod method,
    int concurrentUsers,
    int totalRequests,
    int timeoutMillis,
    Map<String, String> headers,
    String body
) {
    public TestConfig {
        if (concurrentUsers < 1) {
            throw new IllegalArgumentException("concurrentUsers must be at least 1");
        }
        if (totalRequests < 1) {
            throw new IllegalArgumentException("totalRequests must be at least 1");
        }
        if (timeoutMillis < 100) {
            throw new IllegalArgumentException("timeout must be at least 100ms");
        }
    }
}
```

#### TestResult (Domain VO)

```java
public record TestResult(
    int totalRequests,
    int successCount,
    int failCount,
    double avgResponseTime,
    long minResponseTime,
    long maxResponseTime,
    double requestsPerSecond,
    Percentiles percentiles,
    Duration totalDuration
) {
    public record Percentiles(
        long p50,
        long p90,
        long p95,
        long p99
    ) {}
}
```

#### VirtualThreadTestEngine (Infrastructure)

```java
@Component
public class VirtualThreadTestEngine implements TestEngine {
    
    private final HttpRequestPort httpRequestPort;

    @Override
    public TestResult execute(TestConfig config) {
        List<RequestResult> results = new CopyOnWriteArrayList<>();
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = IntStream.range(0, config.totalRequests())
                .mapToObj(i -> executor.submit(() -> {
                    RequestResult result = httpRequestPort.send(toRequestSpec(config));
                    results.add(result);
                    return result;
                }))
                .toList();
            
            // 동시 사용자 수 제한을 위한 Semaphore
            Semaphore semaphore = new Semaphore(config.concurrentUsers());
            
            for (var future : futures) {
                semaphore.acquire();
                future.whenComplete((r, e) -> semaphore.release());
            }
            
            // 모든 요청 완료 대기
            for (var future : futures) {
                future.get();
            }
        }
        
        return aggregateResults(results);
    }
}
```

### 구현 체크리스트

- [ ] 프로젝트 초기 설정 (Gradle, Spring Boot)
- [ ] Domain 모델 구현
    - [ ] LoadTest, LoadTestId
    - [ ] TestConfig, TestResult
    - [ ] TestStatus
- [ ] Application Layer 구현
    - [ ] UseCase 인터페이스 정의
    - [ ] LoadTestService 구현
- [ ] Engine 구현
    - [ ] TestEngine 인터페이스
    - [ ] VirtualThreadTestEngine 구현
    - [ ] JavaHttpClientAdapter 구현
- [ ] Infrastructure 구현
    - [ ] REST Controller
    - [ ] InMemoryRepository
- [ ] 테스트 코드 작성
- [ ] 통합 테스트

---

## Phase 2: Advanced Features

### 목표

실제 사용에 필요한 고급 기능 추가

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 시나리오 테스트 | 여러 API 순차/병렬 호출 | P1 |
| Ramp-up | 점진적 부하 증가 | P1 |
| 시간 기반 테스트 | N초 동안 지속 테스트 | P1 |
| 결과 저장 | JSON/CSV 내보내기 | P1 |
| 테스트 중단 | 실행 중인 테스트 취소 | P1 |
| 테스트 이력 | 과거 테스트 결과 조회 | P2 |
| 인증 지원 | Bearer Token, Basic Auth | P2 |

### 추가 도메인 모델

#### LoadStrategy (Strategy Pattern)

```java
public interface LoadStrategy {
    Stream<ScheduledRequest> schedule(TestConfig config);
}

public class ConstantLoadStrategy implements LoadStrategy {
    // 일정한 부하
}

public class RampUpLoadStrategy implements LoadStrategy {
    private final Duration rampUpDuration;
    // 점진적 증가
}

public class SpikeLoadStrategy implements LoadStrategy {
    // 급격한 부하 증가
}
```

#### TestScenario (시나리오 테스트)

```java
public record TestScenario(
    String name,
    List<TestStep> steps
) {}

public record TestStep(
    String name,
    TestConfig config,
    StepType type,  // SEQUENTIAL, PARALLEL
    Duration delay
) {}
```

### 구현 체크리스트

- [ ] LoadStrategy 패턴 구현
    - [ ] ConstantLoadStrategy
    - [ ] RampUpLoadStrategy
    - [ ] SpikeLoadStrategy
- [ ] 시나리오 테스트
    - [ ] TestScenario 도메인
    - [ ] ScenarioExecutionService
- [ ] 결과 내보내기
    - [ ] JsonExporter
    - [ ] CsvExporter
- [ ] 테스트 중단 기능
- [ ] DB 영속화 (선택)
    - [ ] JPA Entity
    - [ ] Repository 구현

---

## Phase 3: UI & Monitoring

### 목표

사용자 친화적 웹 UI와 실시간 모니터링

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 웹 대시보드 | 테스트 설정 및 실행 UI | P1 |
| 실시간 모니터링 | 진행 상황 실시간 표시 | P1 |
| 차트/그래프 | 응답시간 분포, TPS 추이 | P2 |
| 테스트 히스토리 | 과거 테스트 목록/상세 | P2 |
| 비교 기능 | 여러 테스트 결과 비교 | P3 |

### 기술 선택지

| 옵션 | 기술 | 장점 | 단점 |
|------|------|------|------|
| A | Thymeleaf + HTMX | 단순, SSR | 실시간 제약 |
| B | React (별도 프로젝트) | 유연, 실시간 | 복잡도 증가 |
| C | Vaadin | 풀스택 Java | 러닝커브 |

### 실시간 모니터링 설계

```
┌─────────────┐     SSE/WebSocket     ┌─────────────┐
│   Browser   │◄─────────────────────│   Server    │
│             │                       │             │
│  Dashboard  │     REST API          │  Overload   │
│             │─────────────────────►│             │
└─────────────┘                       └─────────────┘
```

```java
@GetMapping(value = "/tests/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<TestProgress> streamProgress(@PathVariable String id) {
    return testProgressService.getProgressStream(id);
}
```

---

## API 설계

### 엔드포인트 목록

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/v1/tests | 테스트 생성 및 실행 |
| GET | /api/v1/tests | 테스트 목록 조회 |
| GET | /api/v1/tests/{id} | 테스트 상세 조회 |
| GET | /api/v1/tests/{id}/status | 테스트 상태 조회 |
| DELETE | /api/v1/tests/{id} | 테스트 중단 |
| GET | /api/v1/tests/{id}/export | 결과 내보내기 |

### 요청/응답 DTO

#### CreateLoadTestRequest

```java
public record CreateLoadTestRequest(
    @NotBlank String name,
    @NotBlank @URL String targetUrl,
    @NotNull HttpMethod method,
    @Min(1) @Max(10000) int concurrentUsers,
    @Min(1) @Max(1000000) int totalRequests,
    @Min(100) @Max(60000) int timeout,
    Map<String, String> headers,
    String body
) {}
```

#### LoadTestResponse

```java
public record LoadTestResponse(
    String testId,
    String name,
    TestStatus status,
    TestConfigDto config,
    TestResultDto result,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt
) {}
```

#### TestResultDto

```java
public record TestResultDto(
    int totalRequests,
    int successCount,
    int failCount,
    double avgResponseTime,
    long minResponseTime,
    long maxResponseTime,
    double requestsPerSecond,
    PercentilesDto percentiles
) {
    public record PercentilesDto(
        long p50,
        long p90,
        long p95,
        long p99
    ) {}
}
```

---

## 도메인 모델

### 클래스 다이어그램

```
┌─────────────────┐       ┌─────────────────┐
│    LoadTest     │       │   TestConfig    │
├─────────────────┤       ├─────────────────┤
│ - id            │       │ - targetUrl     │
│ - name          │◄──────│ - method        │
│ - config        │       │ - concurrentUsers│
│ - status        │       │ - totalRequests │
│ - result        │       │ - timeout       │
│ - createdAt     │       │ - headers       │
│ - startedAt     │       └─────────────────┘
│ - finishedAt    │
├─────────────────┤       ┌─────────────────┐
│ + create()      │       │   TestResult    │
│ + start()       │       ├─────────────────┤
│ + complete()    │◄──────│ - totalRequests │
│ + fail()        │       │ - successCount  │
└─────────────────┘       │ - failCount     │
                          │ - avgResponseTime│
┌─────────────────┐       │ - percentiles   │
│   TestStatus    │       └─────────────────┘
├─────────────────┤
│ READY           │       ┌─────────────────┐
│ RUNNING         │       │  RequestResult  │
│ COMPLETED       │       ├─────────────────┤
│ FAILED          │       │ - success       │
│ CANCELLED       │       │ - statusCode    │
└─────────────────┘       │ - responseTime  │
                          │ - errorMessage  │
                          └─────────────────┘
```

### 상태 다이어그램

```
          create()         start()
    ┌────────────────┐  ┌──────────────┐
    │                │  │              │
    ▼                │  ▼              │
┌───────┐      ┌─────────┐      ┌─────────┐
│ READY │─────►│ RUNNING │─────►│COMPLETED│
└───────┘      └─────────┘      └─────────┘
                    │
                    │ fail() / cancel()
                    ▼
              ┌───────────┐
              │  FAILED   │
              │ CANCELLED │
              └───────────┘
```

---

## 기술적 고려사항

### Virtual Threads 사용 시 주의점

1. **Pinning 방지**
    - `synchronized` 블록 내 장시간 작업 피하기
    - Java 24+에서는 synchronized pinning 해결됨

2. **Thread Pool 사용하지 않기**
    - Virtual Threads는 풀링하지 않음
    - 매 요청마다 새로운 Virtual Thread 생성

3. **Blocking I/O 활용**
    - Virtual Threads에서는 blocking 코드가 효율적
    - Reactive 스타일 불필요

### 메트릭 수집

```java
// Percentile 계산
public Percentiles calculatePercentiles(List<Long> responseTimes) {
    Collections.sort(responseTimes);
    int size = responseTimes.size();
    
    return new Percentiles(
        responseTimes.get((int) (size * 0.50)),
        responseTimes.get((int) (size * 0.90)),
        responseTimes.get((int) (size * 0.95)),
        responseTimes.get((int) (size * 0.99))
    );
}
```

### 동시성 제어

```java
// Semaphore로 동시 요청 수 제한
Semaphore semaphore = new Semaphore(config.concurrentUsers());

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < config.totalRequests(); i++) {
        semaphore.acquire();
        executor.submit(() -> {
            try {
                return httpRequestPort.send(spec);
            } finally {
                semaphore.release();
            }
        });
    }
}
```

### 에러 처리

```java
public sealed interface RequestResult 
    permits SuccessResult, FailureResult {
}

public record SuccessResult(
    int statusCode,
    long responseTimeMs
) implements RequestResult {}

public record FailureResult(
    String errorMessage,
    ErrorType errorType
) implements RequestResult {}

public enum ErrorType {
    TIMEOUT,
    CONNECTION_REFUSED,
    UNKNOWN
}
```

---

## 마일스톤

| Phase | 목표 기간 | 주요 결과물 |
|-------|----------|------------|
| Phase 1 | 2주 | 동작하는 MVP |
| Phase 2 | 2주 | 고급 기능 추가 |
| Phase 3 | 2주 | 웹 UI 완성 |

---

## 참고

- [프로젝트 README](../../README.md)
- [아키텍처 문서](../architecture/README.md)