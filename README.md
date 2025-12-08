# Overload

**Lightweight HTTP load testing tool powered by Java Virtual Threads**

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 소개

Overload는 Java 21+의 **Virtual Threads**를 활용한 경량 HTTP 부하 테스트 도구입니다.

`wrk`, `hey`, `k6`처럼 커맨드라인에서 간편하게 사용할 수 있으며, **Spring Boot Starter**를 통해 Web UI 대시보드도 제공합니다.  
Virtual Threads의 경량성 덕분에 적은 리소스로 대규모 동시 요청을 생성할 수 있습니다.

### 왜 Overload인가?

| 기존 방식 | Overload |
|-----------|----------|
| OS Thread 기반 (1~4MB/thread) | Virtual Thread 기반 (~1KB/thread) |
| 수백 개 동시 요청 한계 | 수만 개 동시 요청 가능 |
| 복잡한 설치 (Go, Lua 등) | JVM만 있으면 실행 |
| 무거운 설정 | 단일 명령어로 실행 |

### 핵심 원칙: 부하 생성기 ≠ 테스트 대상

```
┌──────────────────────┐          ┌──────────────────────┐
│  Overload            │   HTTP   │  Target Server       │
│  (Load Generator)    │ ──────►  │  (Test Target)       │
│                      │   부하   │                      │
│  별도 서버/프로세스  │          │  api.example.com     │
└──────────────────────┘          └──────────────────────┘
```

---

## 빠른 시작

### 방법 1: CLI 사용

```bash
# 빌드
git clone https://github.com/junhyeong9812/overload.git
cd overload
./gradlew build

# 설치 (PATH에 추가)
./gradlew installDist
export PATH=$PATH:$(pwd)/overload-cli/build/install/overload-cli/bin

# 실행
overload run -u https://httpbin.org/get -c 100 -n 1000
```

### 방법 2: Web UI 사용 (@EnableOverload)

```groovy
// build.gradle
dependencies {
    implementation 'io.github.junhyeong9812:overload-spring-boot-starter:0.1.0'
}
```

```java
@SpringBootApplication
@EnableOverload  // 이것만 추가!
public class LoadTestServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(LoadTestServerApplication.class, args);
  }
}
```

```
http://localhost:8080/overload → Web UI 접속
```

### 방법 3: 시나리오 테스트 (MSA 환경)

여러 API 호출을 순차적으로 연결하는 **시나리오 테스트**를 지원합니다.  
로그인 → 토큰 추출 → API 호출 같은 실제 사용자 흐름을 시뮬레이션할 수 있습니다.

```yaml
# scenario.yaml
name: "User Login Flow"

steps:
  - id: login
    method: POST
    url: https://api.example.com/auth/login
    headers:
      Content-Type: application/json
    body: '{"username": "test", "password": "1234"}'
    extract:
      token: "$.data.accessToken"
      userId: "$.data.userId"

  - id: getProfile
    method: GET
    url: "https://api.example.com/users/${login.userId}"
    headers:
      Authorization: "Bearer ${login.token}"

  - id: updateProfile
    method: PUT
    url: "https://api.example.com/users/${login.userId}"
    headers:
      Authorization: "Bearer ${login.token}"
    body: '{"nickname": "updated"}'

settings:
  failureStrategy: STOP
  retryCount: 3
  retryDelayMs: 1000
```

```bash
# CLI로 시나리오 실행
overload scenario -f scenario.yaml -c 50 -n 1000
```

**Web UI**에서도 **Scenario Test** 탭을 통해 시나리오를 구성하고 실행할 수 있습니다.

### 출력 예시

```
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

### 시나리오 테스트 출력 예시

```
Scenario: User Login Flow
Steps: 3 | Iterations: 1,000 | Concurrency: 50

Step Statistics:
  login         ✅ 98.5% | avg: 120ms | max: 450ms
  getProfile    ✅ 99.2% | avg: 45ms  | max: 180ms
  updateProfile ✅ 97.8% | avg: 85ms  | max: 320ms

Overall:
  Success Rate:    95.5%
  Scenarios/sec:   127.3
  Total Time:      7.86s
```

---

## 주요 기능

- 🚀 **Virtual Threads 기반** - 수만 개 동시 요청을 경량으로 처리
- ⚡ **CLI 우선** - 터미널에서 바로 실행, CI/CD 파이프라인 통합
- 🌐 **Web UI** - `@EnableOverload`로 Postman 스타일 대시보드 활성화
- 📊 **상세 메트릭** - TPS, Percentile(p50/p90/p95/p99), 히스토그램
- 🔗 **시나리오 테스트** - 다단계 API 호출, 변수 추출/치환, 실패 전략
- 📁 **YAML 시나리오** - 복잡한 테스트 시나리오 설정 파일 지원
- 📈 **결과 내보내기** - JSON, CSV 형식 지원
- 🔌 **라이브러리 사용** - Java 프로젝트에서 직접 import 가능
- 🎯 **부하 패턴** - Constant, Ramp-up, Spike, Step 지원

---

## 시나리오 테스트 기능

### 변수 추출 (Extract)

이전 Step의 응답에서 값을 추출하여 다음 Step에서 사용할 수 있습니다.

| 추출 방식 | 문법 | 예시 |
|-----------|------|------|
| JSONPath | `$.path.to.value` | `$.data.accessToken` |
| 배열 접근 | `$.array[index]` | `$.users[0].id` |
| 헤더 | `$header.Name` | `$header.Set-Cookie` |

```yaml
steps:
  - id: login
    url: https://api.example.com/auth/login
    method: POST
    body: '{"username": "test", "password": "1234"}'
    extract:
      token: "$.data.accessToken"
      userId: "$.data.user.id"

  - id: getOrders
    url: "https://api.example.com/users/${login.userId}/orders"
    headers:
      Authorization: "Bearer ${login.token}"
```

### 변수 치환 (Substitution)

`${stepId.variableName}` 형식으로 이전 Step에서 추출한 값을 참조합니다.

- **URL**: `https://api.example.com/users/${login.userId}`
- **Headers**: `Authorization: Bearer ${login.token}`
- **Body**: `{"userId": "${login.userId}"}`

### 실패 전략 (Failure Strategy)

| 전략 | 설명 |
|------|------|
| `STOP` | Step 실패 시 해당 시나리오 즉시 중단 (기본값) |
| `SKIP` | 실패한 Step을 건너뛰고 다음 Step 계속 진행 |
| `RETRY` | 실패 시 지정 횟수만큼 재시도 |

```yaml
settings:
  failureStrategy: RETRY
  retryCount: 3
  retryDelayMs: 1000
```

### Java 코드로 시나리오 실행

```java
import io.github.junhyeong9812.overload.scenario.*;

Scenario scenario = Scenario.builder()
    .name("Order Flow Test")
    .failureStrategy(FailureStrategy.STOP)
    
    .step("login", step -> step
        .post("http://auth-service/api/login")
        .header("Content-Type", "application/json")
        .body("{\"username\":\"test\",\"password\":\"1234\"}")
        .extract("token", "$.data.accessToken")
        .extract("userId", "$.data.user.id"))
    
    .step("getProducts", step -> step
        .get("http://product-service/api/products")
        .header("Authorization", "Bearer ${login.token}")
        .extract("productId", "$.data[0].id"))
    
    .step("createOrder", step -> step
        .post("http://order-service/api/orders")
        .header("Authorization", "Bearer ${login.token}")
        .body("{\"userId\":\"${login.userId}\",\"productId\":\"${getProducts.productId}\"}"))
    
    .build();

ScenarioTestResult result = ScenarioLoadTester.run(
    scenario,
    100,                      // iterations
    10,                       // concurrency
    Duration.ofSeconds(30)    // timeout
);

System.out.println(result.summary());
```

---

## CLI 옵션

### `overload run`

```
Usage: overload run [OPTIONS]

HTTP 부하 테스트를 실행합니다.

Options:
  -u, --url <URL>           대상 URL (필수)
  -m, --method <METHOD>     HTTP 메서드 (기본: GET)
  -c, --concurrency <N>     동시 요청 수 (기본: 10)
  -n, --requests <N>        총 요청 수 (기본: 100)
  -t, --duration <SEC>      테스트 지속 시간 (요청 수 대신 사용)
  -H, --header <HEADER>     HTTP 헤더 (여러 번 사용 가능)
  -d, --data <BODY>         요청 본문
  --timeout <MS>            요청 타임아웃 (기본: 5000ms)
  -f, --file <PATH>         YAML 설정 파일
  -o, --output <FORMAT>     출력 형식 (text, json, csv)
  --no-color                색상 출력 비활성화
  -q, --quiet               최소 출력
  -v, --verbose             상세 출력
  -h, --help                도움말 출력

Examples:
  # 간단한 GET 요청
  overload run -u https://httpbin.org/get -c 100 -n 1000

  # POST 요청
  overload run -u https://api.example.com/users \
    -m POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer token123" \
    -d '{"name": "test"}' \
    -c 50 -n 500

  # YAML 설정 파일 사용
  overload run -f scenario.yaml
```

### `overload scenario`

```
Usage: overload scenario [OPTIONS]

시나리오 기반 부하 테스트를 실행합니다.

Options:
  -f, --file <PATH>         시나리오 YAML 파일 (필수)
  -c, --concurrency <N>     동시 시나리오 수 (기본: 10)
  -n, --iterations <N>      총 반복 수 (기본: 100)
  --timeout <MS>            요청 타임아웃 (기본: 30000ms)
  -o, --output <FORMAT>     출력 형식 (text, json)
  -h, --help                도움말 출력

Examples:
  # 시나리오 실행
  overload scenario -f login-flow.yaml -c 50 -n 1000
```

---

## 설정 파일 (YAML)

### 단순 부하 테스트

```yaml
# load-test.yaml
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

### 시나리오 테스트

```yaml
# scenario.yaml
name: "E-Commerce Order Flow"

steps:
  - id: login
    method: POST
    url: https://api.example.com/auth/login
    headers:
      Content-Type: application/json
    body: '{"username": "test", "password": "1234"}'
    extract:
      token: "$.data.accessToken"
      userId: "$.data.userId"

  - id: getCart
    method: GET
    url: "https://api.example.com/users/${login.userId}/cart"
    headers:
      Authorization: "Bearer ${login.token}"
    extract:
      cartId: "$.data.id"

  - id: checkout
    method: POST
    url: "https://api.example.com/orders"
    headers:
      Authorization: "Bearer ${login.token}"
      Content-Type: application/json
    body: |
      {
        "cartId": "${getCart.cartId}",
        "paymentMethod": "card"
      }

settings:
  failureStrategy: STOP
  retryCount: 0
  retryDelayMs: 1000

load:
  concurrency: 50
  iterations: 1000
  timeout: 30000
```

```bash
# 환경 변수와 함께 실행
TOKEN=my-secret-token overload run -f scenario.yaml
```

---

## Web UI 설정

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

**Web UI 기능:**
- **Load Test 탭**: 단순 HTTP 부하 테스트
- **Scenario Test 탭**: 다단계 시나리오 테스트
- **실시간 진행률**: WebSocket 기반 라이브 업데이트
- **결과 시각화**: 성공률, TPS, Step별 통계

---

## 라이브러리로 사용

Maven/Gradle 의존성으로 추가하여 Java 코드에서 직접 사용할 수 있습니다.

### 단순 부하 테스트

```groovy
// build.gradle
dependencies {
    implementation 'io.github.junhyeong9812:overload-core:0.1.0'
}
```

```java
import io.github.junhyeong9812.overload.core.*;

LoadTestConfig config = LoadTestConfig.builder()
    .url("https://api.example.com/users")
    .method(HttpMethod.GET)
    .concurrency(100)
    .totalRequests(10000)
    .timeout(Duration.ofSeconds(5))
    .build();

TestResult result = LoadTester.run(config, progress -> {
    System.out.printf("Progress: %d/%d%n", progress.completed(), progress.total());
});

System.out.println("TPS: " + result.requestsPerSecond());
System.out.println("p99: " + result.latencyStats().percentiles().p99() + "ms");
```

### 시나리오 테스트

```groovy
// build.gradle
dependencies {
    implementation 'io.github.junhyeong9812:overload-scenario:0.1.0'
}
```

```java
import io.github.junhyeong9812.overload.scenario.*;

Scenario scenario = Scenario.builder()
    .name("API Flow Test")
    .step("step1", s -> s.get("http://api/endpoint1").extract("id", "$.data.id"))
    .step("step2", s -> s.post("http://api/endpoint2/${step1.id}"))
    .build();

ScenarioTestResult result = ScenarioLoadTester.run(scenario, 100, 10, Duration.ofSeconds(30));

System.out.println("Success Rate: " + result.successRate() + "%");
System.out.println("Scenarios/sec: " + result.scenariosPerSecond());
```

---

## 프로젝트 구조

```
overload/
├── overload-core/                  # 핵심 엔진 (순수 Java, Hexagonal)
│   └── src/main/java/
│       └── io.github.junhyeong9812.overload.core/
│           ├── engine/             # Virtual Thread 실행 엔진
│           ├── http/               # HTTP 클라이언트
│           ├── metric/             # 메트릭 수집/계산
│           └── config/             # 설정 모델
│
├── overload-scenario/              # 시나리오 테스트 모듈
│   └── src/main/java/
│       └── io.github.junhyeong9812.overload.scenario/
│           ├── scenario/           # 시나리오 도메인, 실행기
│           ├── variable/           # 변수 추출/치환
│           └── builder/            # DSL 빌더
│
├── overload-cli/                   # CLI 도구 (picocli)
│   └── src/main/java/
│       └── io.github.junhyeong9812.overload.cli/
│           ├── command/            # CLI 명령어
│           ├── output/             # 출력 포매터
│           └── progress/           # 진행률 표시
│
├── overload-spring-boot-starter/   # Web UI (@EnableOverload)
│   └── src/main/java/
│       └── io.github.junhyeong9812.overload.starter/
│           ├── controller/         # REST API, 대시보드
│           ├── service/            # 부하 테스트 서비스
│           ├── scenario/           # 시나리오 테스트 API
│           └── websocket/          # 실시간 결과
│
├── docs/
│   ├── architecture/               # 아키텍처 문서
│   └── implementation/             # 구현 계획
│
├── build.gradle
└── settings.gradle
```

> 아키텍처에 대한 자세한 내용은 [아키텍처 문서](docs/architecture/README.md)를 참고하세요.

---

## 기술 스택

| 모듈 | 기술 | 버전 |
|------|------|------|
| **overload-core** | Java, Virtual Threads, JDK HttpClient | 21 |
| **overload-scenario** | Java, JSONPath (json-path) | 21 |
| **overload-cli** | picocli, jansi, snakeyaml | Java 21 |
| **overload-spring-boot-starter** | Spring Boot, WebSocket, Thymeleaf | 3.5.x |
| **Build** | Gradle (멀티 모듈) | 8.11.1 |

---

## 로드맵

- [x] 프로젝트 초기 설계
- [x] **Phase 1: Core + CLI**
    - [x] overload-core 구현
    - [x] overload-cli 구현
    - [x] 기본 테스트 실행
- [x] **Phase 2: Scenario + Advanced Features**
    - [x] overload-scenario 모듈 구현
    - [x] 변수 추출/치환 (JSONPath)
    - [ ] YAML 시나리오 파싱
    - [ ] Ramp-up 부하 패턴
    - [ ] 결과 내보내기 (JSON, CSV)
- [x] **Phase 3: Spring Boot Starter**
    - [x] @EnableOverload 어노테이션
    - [x] Web UI 대시보드
    - [x] 실시간 모니터링 (WebSocket)
    - [x] Scenario Test 탭

> 상세 로드맵은 [구현 계획](docs/implementation/README.md)을 참고하세요.

---

## 비교

| 도구 | 언어 | CLI | Web UI | 시나리오 | 특징 |
|------|------|-----|--------|----------|------|
| **Overload** | Java (Virtual Threads) | ✅ | ✅ | ✅ | JVM 환경, 경량, MSA 지원 |
| wrk | C + Lua | ✅ | ❌ | ❌ | 고성능, Lua 스크립트 |
| hey | Go | ✅ | ❌ | ❌ | 간단한 CLI |
| k6 | Go + JS | ✅ | ❌ | ✅ | JavaScript 시나리오, 클라우드 |
| JMeter | Java | ✅ | ✅ | ✅ | GUI 기반, 복잡한 설정 |
| Gatling | Scala | ✅ | ❌ | ✅ | 코드 기반, 상세 리포트 |

---

## 문서

| 문서 | 설명 |
|------|------|
| [아키텍처](docs/architecture/README.md) | 멀티 모듈 구조 설명 |
| [구현 계획](docs/implementation/README.md) | 단계별 구현 계획 |
| [overload-core 설계](docs/implementation/overload-core.md) | Core 모듈 상세 설계 |
| [overload-scenario 설계](docs/implementation/overload-scenario.md) | Scenario 모듈 상세 설계 |
| [overload-cli 설계](docs/implementation/overload-cli.md) | CLI 모듈 상세 설계 |
| [overload-starter 설계](docs/implementation/overload-starter.md) | Starter 모듈 상세 설계 |

---

## ⚠️ 사용 주의사항

**Usage Disclaimer**

This tool is intended *only* for performance testing of services you own or are explicitly authorized to test.

Unauthorized load testing against external systems may be illegal and subject to criminal charges.

By using Overload, you agree that you are responsible for the usage and compliance with applicable laws and regulations.

---

**사용 주의사항**

Overload는 사용자 본인이 소유하거나 명시적 허가를 받은 서버에 대한 성능/부하 테스트 목적에만 사용해야 합니다.

허가 없이 타인 서비스에 부하 테스트를 수행하면 관련 법률에 따라 민형사 처벌 대상이 될 수 있습니다.

본 도구의 사용으로 인해 발생하는 모든 책임은 사용자에게 있습니다.

---

## 기여

기여는 언제나 환영합니다! Issue와 Pull Request를 통해 참여해주세요.

---

## 라이선스

MIT License - 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 연락처

- GitHub: [@junhyeong9812](https://github.com/junhyeong9812)