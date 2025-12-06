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

---

## 주요 기능

- 🚀 **Virtual Threads 기반** - 수만 개 동시 요청을 경량으로 처리
- ⚡ **CLI 우선** - 터미널에서 바로 실행, CI/CD 파이프라인 통합
- 🌐 **Web UI** - `@EnableOverload`로 Postman 스타일 대시보드 활성화
- 📊 **상세 메트릭** - TPS, Percentile(p50/p90/p95/p99), 히스토그램
- 📁 **YAML 시나리오** - 복잡한 테스트 시나리오 설정 파일 지원
- 📈 **결과 내보내기** - JSON, CSV 형식 지원
- 🔌 **라이브러리 사용** - Java 프로젝트에서 직접 import 가능
- 🎯 **부하 패턴** - Constant, Ramp-up, Spike, Step 지원

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

---

## 설정 파일 (YAML)

```yaml
# scenario.yaml
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

---

## 라이브러리로 사용

Maven/Gradle 의존성으로 추가하여 Java 코드에서 직접 사용할 수 있습니다.

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
| **overload-cli** | picocli, jansi, snakeyaml | Java 25 |
| **overload-spring-boot-starter** | Spring Boot, WebSocket, Thymeleaf | 3.4.x |
| **Build** | Gradle (멀티 모듈) | 8.11.1 |

---

## 로드맵

- [x] 프로젝트 초기 설계
- [ ] **Phase 1: Core + CLI**
    - [ ] overload-core 구현
    - [ ] overload-cli 구현
    - [ ] 기본 테스트 실행
- [ ] **Phase 2: Advanced Features**
    - [ ] YAML 시나리오 지원
    - [ ] Ramp-up 부하 패턴
    - [ ] 결과 내보내기 (JSON, CSV)
- [ ] **Phase 3: Spring Boot Starter**
    - [ ] @EnableOverload 어노테이션
    - [ ] Web UI 대시보드
    - [ ] 실시간 모니터링

> 상세 로드맵은 [구현 계획](docs/implementation/README.md)을 참고하세요.

---

## 비교

| 도구 | 언어 | CLI | Web UI | 특징 |
|------|------|-----|--------|------|
| **Overload** | Java (Virtual Threads) | ✅ | ✅ | JVM 환경, 경량, 라이브러리 사용 가능 |
| wrk | C + Lua | ✅ | ❌ | 고성능, Lua 스크립트 |
| hey | Go | ✅ | ❌ | 간단한 CLI |
| k6 | Go + JS | ✅ | ❌ | JavaScript 시나리오, 클라우드 지원 |
| JMeter | Java | ✅ | ✅ | GUI 기반, 복잡한 시나리오 |
| Gatling | Scala | ✅ | ❌ | 코드 기반, 상세 리포트 |

---

## 문서

| 문서 | 설명 |
|------|------|
| [아키텍처](docs/architecture/README.md) | 멀티 모듈 구조 설명 |
| [구현 계획](docs/implementation/README.md) | 단계별 구현 계획 |
| [overload-core 설계](docs/implementation/overload-core.md) | Core 모듈 상세 설계 |
| [overload-cli 설계](docs/implementation/overload-cli.md) | CLI 모듈 상세 설계 |
| [overload-starter 설계](docs/implementation/overload-starter.md) | Starter 모듈 상세 설계 |

---

## 기여

기여는 언제나 환영합니다! Issue와 Pull Request를 통해 참여해주세요.

---

## 라이선스

MIT License - 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 연락처

- GitHub: [@junhyeong9812](https://github.com/junhyeong9812)