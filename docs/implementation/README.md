# 구현 계획

이 문서는 Overload 프로젝트의 단계별 구현 계획을 담고 있습니다.

---

## 목차

1. [개발 환경](#개발-환경)
2. [Phase 1: Core + CLI](#phase-1-core--cli)
3. [Phase 2: Advanced Features](#phase-2-advanced-features)
4. [Phase 3: Spring Boot Starter](#phase-3-spring-boot-starter)
5. [기술적 고려사항](#기술적-고려사항)
6. [마일스톤](#마일스톤)

---

## 개발 환경

### 기술 스택

| 모듈 | 기술 | 버전 |
|------|------|------|
| **overload-core** | Java (Virtual Threads) | 21 |
| | JDK HttpClient | 내장 |
| **overload-cli** | picocli | 4.7.6 |
| | SnakeYAML | 2.3 |
| | jansi (터미널 색상) | 2.4.1 |
| **overload-spring-boot-starter** | Spring Boot | 3.4.1 |
| | Thymeleaf | 3.1.x |
| | WebSocket | 내장 |
| **Build** | Gradle | 8.11.1 |

---

## Phase 1: Core + CLI

### 목표

**동작하는 CLI 부하 테스트 도구 완성**

```bash
$ overload run -u https://httpbin.org/get -c 100 -n 1000

Overload v0.1.0 - Virtual Thread Load Tester

Target:        https://httpbin.org/get
Method:        GET
Concurrency:   100 virtual threads
Requests:      1,000

Running... ████████████████████ 100% (1,000/1,000)

Results:
  Total Requests:    1,000
  Successful:        985 (98.5%)
  Failed:            15 (1.5%)
  Total Time:        2.34s
  Requests/sec:      427.35

Latency Distribution:
  Min:       12ms
  Max:       892ms
  Avg:       156ms

  p50:       120ms
  p90:       280ms
  p95:       450ms
  p99:       720ms
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
| 타임아웃 | `--timeout` 옵션 | P1 |

### 구현 순서

```
1. overload-core (2주)
   ├── 1-1. config 패키지 (1일)
   │   ├── HttpMethod.java
   │   └── LoadTestConfig.java
   │
   ├── 1-2. http 패키지 (2일)
   │   ├── domain/HttpRequest.java
   │   ├── domain/HttpResponse.java
   │   ├── domain/RequestResult.java
   │   ├── application/port/HttpClientPort.java
   │   └── infrastructure/JdkHttpClient.java
   │
   ├── 1-3. metric 패키지 (2일)
   │   ├── domain/Percentiles.java
   │   ├── domain/LatencyHistogram.java
   │   ├── domain/TestResult.java
   │   └── application/MetricAggregator.java
   │
   ├── 1-4. engine 패키지 (3일)
   │   ├── domain/LoadTestEngine.java
   │   ├── domain/ExecutionContext.java
   │   ├── application/TestExecutor.java
   │   └── infrastructure/VirtualThreadEngine.java
   │
   ├── 1-5. LoadTester.java (1일)
   │
   └── 1-6. 테스트 작성 (3일)

2. overload-cli (1주)
   ├── 2-1. command 패키지 (2일)
   │   ├── RootCommand.java
   │   └── RunCommand.java
   │
   ├── 2-2. output 패키지 (1일)
   │   ├── OutputFormatter.java
   │   ├── TextFormatter.java
   │   └── JsonFormatter.java
   │
   ├── 2-3. progress 패키지 (1일)
   │   └── ProgressBar.java
   │
   ├── 2-4. Main.java (0.5일)
   │
   └── 2-5. Fat JAR 빌드 설정 (0.5일)
```

### 상세 문서

- [overload-core 구현 계획](./overload-core.md)
- [overload-cli 구현 계획](./overload-cli.md)

---

## Phase 2: Advanced Features

### 목표

실제 사용에 필요한 고급 기능 추가

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| YAML 설정 | `-f scenario.yaml`로 설정 로드 | P0 |
| 시간 기반 테스트 | `-t 60s`로 N초 동안 테스트 | P0 |
| Ramp-up | 점진적 부하 증가 | P1 |
| 결과 내보내기 | `--output csv`, JSON/CSV 파일 저장 | P1 |
| 환경 변수 | YAML에서 `${VAR}` 치환 | P1 |

### YAML 설정 예시

```yaml
name: "User API Load Test"

target:
  url: https://api.example.com/users
  method: POST
  headers:
    Authorization: "Bearer ${TOKEN}"
    Content-Type: "application/json"
  body: |
    {
      "name": "test",
      "email": "test@example.com"
    }

load:
  concurrency: 100
  requests: 10000
  # 또는 duration: 60s
  rampUp: 10s

options:
  timeout: 5000

output:
  format: json
  file: results.json
```

### 부하 패턴 (LoadStrategy)

#### Constant (기본)
```
Concurrency
    │
100 ├────────────────────────
    │
  0 └────────────────────────► Time
```

#### Ramp-up
```
Concurrency
    │              ┌─────────
100 ├             ╱
    │           ╱
 50 ├         ╱
    │       ╱
  0 └─────╱──────────────────► Time
```

#### Spike
```
Concurrency
    │     ╱╲
500 ├    ╱  ╲
    │   ╱    ╲
100 ├──╱      ╲──────────────
    │
  0 └────────────────────────► Time
```

#### Step
```
Concurrency
    │              ┌─────────
300 ├             │
    │        ┌────┘
200 ├       │
    │  ┌────┘
100 ├──┘
    │
  0 └────────────────────────► Time
```

### 구현 체크리스트

- [ ] YAML 설정 로더
    - [ ] YamlConfigLoader
    - [ ] 환경 변수 치환
- [ ] 시간 기반 테스트
    - [ ] DurationBasedExecutor
- [ ] LoadStrategy 패턴
    - [ ] ConstantLoadStrategy
    - [ ] RampUpLoadStrategy
    - [ ] SpikeLoadStrategy (선택)
    - [ ] StepLoadStrategy (선택)
- [ ] 결과 내보내기
    - [ ] CsvFormatter
    - [ ] 파일 저장 옵션

---

## Phase 3: Spring Boot Starter

### 목표

`@EnableOverload`로 Web UI 부하 테스트 서버 활성화

### 기능 목록

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| @EnableOverload | 어노테이션으로 활성화 | P0 |
| 대시보드 UI | Postman 스타일 요청 빌더 | P0 |
| 부하 테스트 실행 | REST API로 테스트 실행 | P0 |
| 실시간 결과 | WebSocket으로 진행 상황 | P1 |
| 테스트 이력 | 과거 테스트 결과 조회 | P2 |

### 사용 예시

```java
@SpringBootApplication
@EnableOverload
public class LoadTestServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoadTestServerApplication.class, args);
    }
}
```

```yaml
# application.yml
server:
  port: 9090

overload:
  enabled: true
  dashboard:
    path: /overload
    title: "My Load Tester"
  defaults:
    concurrency: 10
    timeout: 30s
  security:
    enabled: true
    username: admin
    password: admin123
```

### REST API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /overload | 대시보드 UI |
| POST | /overload/api/tests | 테스트 실행 |
| GET | /overload/api/tests/{id} | 테스트 상태 조회 |
| DELETE | /overload/api/tests/{id} | 테스트 중지 |
| GET | /overload/api/tests | 테스트 이력 |
| WS | /overload/ws | 실시간 결과 |

### 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.4 |
| Template | Thymeleaf |
| Styling | Tailwind CSS / HTMX |
| Charts | Chart.js |
| Realtime | WebSocket / SSE |

### 구현 체크리스트

- [ ] @EnableOverload 어노테이션
- [ ] OverloadAutoConfiguration
- [ ] OverloadProperties
- [ ] OverloadDashboardController
- [ ] OverloadApiController
- [ ] LoadTestService
- [ ] WebSocket 핸들러
- [ ] dashboard.html (Thymeleaf)

### 상세 문서

- [overload-spring-boot-starter 구현 계획](./overload-starter.md)

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
- Thread Pool 사용하지 않기 (Virtual Thread는 풀링 불필요)

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

### Thread-safe 메트릭 수집

```java
// LongAdder 사용 (Lock-free)
private final LongAdder totalRequests = new LongAdder();
private final LongAdder successCount = new LongAdder();
private final LongAdder failureCount = new LongAdder();

public void record(RequestResult result) {
    totalRequests.increment();
    if (result instanceof RequestResult.Success) {
        successCount.increment();
    } else {
        failureCount.increment();
    }
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
| Phase 1 | 3주 | 동작하는 CLI 도구 |
| Phase 2 | 2주 | YAML, Ramp-up, 내보내기 |
| Phase 3 | 2주 | @EnableOverload Web UI |

---

## 상세 문서

| 문서 | 설명 |
|------|------|
| [overload-core](./overload-core.md) | Core 모듈 상세 설계 (Hexagonal) |
| [overload-cli](./overload-cli.md) | CLI 모듈 상세 설계 |
| [overload-starter](./overload-starter.md) | Starter 모듈 상세 설계 |

---

## 참고

- [프로젝트 README](../../README.md)
- [아키텍처 문서](../architecture/README.md)
- [picocli 문서](https://picocli.info/)
- [Virtual Threads - JEP 444](https://openjdk.org/jeps/444)