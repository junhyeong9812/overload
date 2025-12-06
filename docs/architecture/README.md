# 아키텍처

이 문서는 Overload 프로젝트의 아키텍처 구조와 설계 철학을 설명합니다.

---

## 개요

Overload는 **멀티 모듈 구조**로 관심사를 분리합니다.

```
overload/
├── overload-core/                  # 순수 Java 엔진 (Hexagonal)
├── overload-cli/                   # CLI 도구 (단순 구조)
└── overload-spring-boot-starter/   # Web UI (Spring 관례)
```

### 아키텍처 결정

| 모듈 | 아키텍처 | 이유 |
|------|----------|------|
| **overload-core** | Hexagonal | 복잡한 비즈니스 로직, HTTP 클라이언트 교체 가능성 |
| **overload-cli** | 단순 구조 | 얇은 레이어, picocli 래핑만 담당 |
| **overload-starter** | Spring 관례 | Spring Boot의 표준 구조 따름 |

---

## 핵심 원칙: 부하 생성기 ≠ 테스트 대상

```
┌──────────────────────┐          ┌──────────────────────┐
│  Overload            │   HTTP   │  Target Server       │
│  (Load Generator)    │ ──────►  │  (Test Target)       │
│                      │   부하   │                      │
│  별도 머신/프로세스  │          │  api.example.com     │
└──────────────────────┘          └──────────────────────┘
        ✅                               ✅
   리소스 충분                      순수 성능 측정
```

**같은 머신에서 부하 생성기와 테스트 대상을 함께 실행하면:**
- 리소스 경쟁으로 부정확한 결과
- 부하 생성기가 CPU/RAM을 소비하여 애플리케이션 성능 왜곡
- 병목이 어디서 발생하는지 판별 불가

---

## 왜 멀티 모듈인가?

### CLI 도구로서의 본질

Overload는 `wrk`, `hey`, `k6`처럼 **CLI 도구**입니다.

```bash
# 이렇게 사용하는 게 목표
overload run -u https://api.example.com -c 100 -n 10000
```

웹 애플리케이션이 아니라 **도구**입니다. 따라서:

| 관심사 | 분리 방법 |
|--------|----------|
| 핵심 엔진 로직 | `overload-core` (순수 Java) |
| CLI 인터페이스 | `overload-cli` (picocli) |
| Web UI | `overload-spring-boot-starter` (Spring Boot) |

### 분리의 이점

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│    ┌──────────────┐     ┌──────────────────────────┐      │
│    │ overload-cli │     │ overload-spring-boot-    │      │
│    │   (picocli)  │     │       starter            │      │
│    └──────┬───────┘     └──────────┬───────────────┘      │
│           │                        │                      │
│           │    의존                │    의존               │
│           ▼                        ▼                      │
│    ┌─────────────────────────────────────────┐            │
│    │          overload-core                  │            │
│    │         (순수 Java 엔진)                │            │
│    │                                         │            │
│    │  • Virtual Threads 실행 엔진            │            │
│    │  • HTTP 클라이언트                      │            │
│    │  • 메트릭 수집/계산                     │            │
│    │  • 설정 모델                            │            │
│    │                                         │            │
│    │  의존성: JDK만 (Spring 없음)            │            │
│    └─────────────────────────────────────────┘            │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

1. **core가 독립적** - Spring 없이 순수 Java로 동작
2. **라이브러리로 사용 가능** - 다른 프로젝트에서 import
3. **CLI와 Web이 같은 core 사용** - 코드 중복 없음
4. **테스트 용이** - core만 단위 테스트 가능

---

## 모듈 간 의존성

```
                    ┌──────────────┐
                    │ overload-cli │
                    │  (Java 25)   │
                    └──────┬───────┘
                           │
                           │ 의존
                           ▼
┌────────────────────┐    ┌──────────────┐
│ overload-spring-   │───▶│overload-core │
│ boot-starter       │    │  (Java 21)   │
│ (Java 21)          │    └──────────────┘
└────────────────────┘           │
        │                        │ 의존 없음
        │ 의존                   ▼
        ▼                    JDK만
   Spring Boot 3.4
```

| 모듈 | 의존 대상 | 외부 의존성 |
|------|----------|------------|
| **overload-core** | 없음 | JDK만 |
| **overload-cli** | overload-core | picocli, jansi, snakeyaml |
| **overload-spring-boot-starter** | overload-core | Spring Boot |

---

## 왜 Core만 Hexagonal인가?

### Hexagonal이 가치 있는 조건

1. **복잡한 비즈니스 로직**이 있는가?
2. **교체 가능한 외부 의존성**이 있는가?

| 모듈 | 복잡한 비즈니스 로직? | 교체 가능한 외부 의존성? | Hexagonal 필요? |
|------|---------------------|------------------------|----------------|
| **overload-core** | ✅ 있음 | ✅ HTTP Client 교체 가능 | ✅ 필요 |
| **overload-cli** | ❌ 없음 | ❌ picocli 교체할 일 없음 | ❌ 불필요 |
| **overload-starter** | ❌ 없음 | ❌ Spring 교체할 일 없음 | ❌ 불필요 |

### CLI와 Starter는 이미 "어댑터"

전체 시스템 관점에서 보면:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Hexagonal 전체 시스템                        │
│                                                                 │
│   ┌───────────────┐                     ┌───────────────┐      │
│   │ overload-cli  │                     │ overload-     │      │
│   │               │                     │ starter       │      │
│   │  (Input       │                     │               │      │
│   │   Adapter)    │                     │ (Input        │      │
│   └───────┬───────┘                     │  Adapter)     │      │
│           │                             └───────┬───────┘      │
│           │                                     │              │
│           ▼                                     ▼              │
│   ┌─────────────────────────────────────────────────────┐      │
│   │                  overload-core                      │      │
│   │                                                     │      │
│   │  ┌─────────────────────────────────────────────┐   │      │
│   │  │              Domain (핵심 로직)              │   │      │
│   │  └─────────────────────────────────────────────┘   │      │
│   │                        │                           │      │
│   │                        ▼                           │      │
│   │  ┌─────────────────────────────────────────────┐   │      │
│   │  │         JdkHttpClient (Output Adapter)      │   │      │
│   │  └─────────────────────────────────────────────┘   │      │
│   └─────────────────────────────────────────────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**CLI와 Starter 자체가 이미 "Input Adapter"입니다.**  
어댑터 내부를 또 Hexagonal로 나눌 필요가 없습니다.

### Core에서 Hexagonal의 가치

```java
// HTTP 클라이언트를 추상화 (Port)
public interface HttpClientPort {
    RequestResult send(HttpRequest request);
}

// JDK 구현 (Adapter)
public class JdkHttpClient implements HttpClientPort { ... }

// 나중에 OkHttp로 교체 가능
public class OkHttpClient implements HttpClientPort { ... }
```

**Core에서 Hexagonal의 가치:**
1. **HTTP 클라이언트 교체 가능** - JDK → OkHttp, Apache HttpClient
2. **테스트 용이** - Mock HttpClientPort 주입
3. **핵심 로직 보호** - 인프라 변경이 도메인에 영향 없음

### CLI가 단순한 이유

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

**CLI는 "얇은 레이어"입니다:**
- 복잡한 로직 없음 (모든 로직은 core에)
- picocli를 다른 CLI 프레임워크로 교체할 일이 거의 없음
- 교체하더라도 영향 범위가 CLI 모듈 내로 한정됨

---

## 모듈별 구조

### overload-core (Hexagonal Architecture)

```
overload-core/
└── src/main/java/io/github/junhyeong9812/overload/core/
    │
    ├── LoadTester.java                 # Facade (진입점)
    │
    ├── config/                         # 설정 모델
    │   ├── LoadTestConfig.java
    │   └── HttpMethod.java
    │
    ├── engine/                         # 테스트 실행 엔진
    │   ├── domain/
    │   │   ├── LoadTestEngine.java     # 엔진 인터페이스
    │   │   └── ExecutionContext.java
    │   ├── application/
    │   │   └── TestExecutor.java
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
    │   │       └── HttpClientPort.java  # 추상화 (교체 가능)
    │   └── infrastructure/
    │       └── JdkHttpClient.java       # JDK 구현
    │
    └── metric/                         # 메트릭 수집/계산
        ├── domain/
        │   ├── TestResult.java
        │   ├── Percentiles.java
        │   └── LatencyHistogram.java
        └── application/
            └── MetricAggregator.java
```

**의존성 규칙:**
```
infrastructure ──────▶ application ──────▶ domain
     │                      │                 │
   Adapter              Port/Service      Pure Java
   (구현체)              (인터페이스)       (POJO)
```

### overload-cli (단순 구조)

```
overload-cli/
└── src/main/java/io/github/junhyeong9812/overload/cli/
    │
    ├── Main.java                       # 진입점
    │
    ├── command/                        # CLI 명령어
    │   ├── RootCommand.java
    │   └── RunCommand.java
    │
    ├── config/                         # 설정 로더
    │   └── YamlConfigLoader.java
    │
    ├── output/                         # 출력 포매터
    │   ├── OutputFormatter.java
    │   ├── TextFormatter.java
    │   ├── JsonFormatter.java
    │   └── CsvFormatter.java
    │
    └── progress/                       # 진행률 표시
        └── ProgressBar.java
```

**단순한 이유:** CLI는 Core를 호출하는 "얇은 래퍼"

### overload-spring-boot-starter (Spring 관례)

```
overload-spring-boot-starter/
└── src/main/java/io/github/junhyeong9812/overload/starter/
    │
    ├── EnableOverload.java             # @EnableOverload 어노테이션
    ├── OverloadAutoConfiguration.java  # Auto-configuration
    ├── OverloadProperties.java         # 설정 프로퍼티
    │
    ├── controller/                     # 웹 레이어
    │   ├── OverloadDashboardController.java
    │   └── OverloadApiController.java
    │
    ├── service/                        # 서비스 레이어
    │   ├── LoadTestService.java
    │   └── ResultBroadcastService.java
    │
    └── websocket/                      # 실시간 통신
        └── OverloadWebSocketHandler.java
```

**Spring 관례를 따르는 이유:**
- Spring의 표준 구조 (Controller → Service)
- `@EnableXxx`, `@AutoConfiguration` 패턴
- 복잡한 로직은 Core에 위임

---

## 기술 스택

### overload-core

| 기술 | 용도 |
|------|------|
| Virtual Threads (JEP 444) | 경량 동시성 |
| java.net.http.HttpClient | HTTP 요청 |
| java.util.concurrent | 동시성 유틸 |
| LongAdder | 락-프리 카운터 |

### overload-cli

| 기술 | 용도 |
|------|------|
| picocli | CLI 프레임워크 |
| snakeyaml | YAML 파싱 |
| jansi | 터미널 색상 |

### overload-spring-boot-starter

| 기술 | 용도 |
|------|------|
| Spring Boot 3.4 | 프레임워크 |
| Spring WebSocket | 실시간 통신 |
| Thymeleaf | 서버사이드 렌더링 |

---

## 배포 형태

```
overload-core-0.1.0.jar              # Maven Central (라이브러리)
overload-cli-0.1.0-all.jar           # Fat JAR (독립 실행)
overload-spring-boot-starter-0.1.0.jar  # Maven Central (Starter)
```

---

## 참고 자료

- [Hexagonal Architecture - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [picocli - CLI Library](https://picocli.info/)
- [Virtual Threads - JEP 444](https://openjdk.org/jeps/444)