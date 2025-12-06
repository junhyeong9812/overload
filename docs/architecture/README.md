# 아키텍처 철학: 멀티 모듈 + Hexagonal

이 문서는 Overload 프로젝트에서 채택한 아키텍처 구조의 철학과 원칙을 설명합니다.

---

## 개요

Overload는 **멀티 모듈 구조**로 관심사를 분리하고, 각 모듈 내부에서는 **Hexagonal Architecture**를 적용합니다.

```
overload/
├── overload-core/      # 순수 Java 엔진 (라이브러리)
├── overload-cli/       # CLI 도구
└── overload-web/       # 웹 대시보드 (선택적)
```

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
| 웹 UI (선택적) | `overload-web` (Spring Boot) |

### 분리의 이점

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│    ┌──────────────┐     ┌──────────────┐                  │
│    │ overload-cli │     │ overload-web │                  │
│    │   (picocli)  │     │ (Spring Boot)│                  │
│    └──────┬───────┘     └──────┬───────┘                  │
│           │                    │                          │
│           │    의존            │    의존                   │
│           ▼                    ▼                          │
│    ┌─────────────────────────────────────┐                │
│    │          overload-core              │                │
│    │         (순수 Java 엔진)             │                │
│    │                                     │                │
│    │  • Virtual Threads 실행 엔진         │                │
│    │  • HTTP 클라이언트                   │                │
│    │  • 메트릭 수집/계산                  │                │
│    │  • 설정 모델                         │                │
│    │                                     │                │
│    │  의존성: JDK만 (Spring 없음)         │                │
│    └─────────────────────────────────────┘                │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

1. **core가 독립적** - Spring 없이 순수 Java로 동작
2. **라이브러리로 사용 가능** - 다른 프로젝트에서 import
3. **CLI와 Web이 같은 core 사용** - 코드 중복 없음
4. **테스트 용이** - core만 단위 테스트 가능

---

## 모듈 구조

### overload-core (핵심 엔진)

**역할:** 부하 테스트 실행 엔진, 순수 Java 라이브러리

**의존성:** JDK만 (외부 의존성 최소화)

```
overload-core/
└── src/main/java/io/github/junhyeong9812/overload/core/
    │
    ├── engine/                         # 테스트 실행 엔진
    │   ├── domain/
    │   │   ├── LoadTestEngine.java     # 엔진 인터페이스
    │   │   ├── ExecutionContext.java
    │   │   └── LoadStrategy.java       # 부하 전략 (Constant, Ramp-up)
    │   ├── application/
    │   │   └── TestExecutor.java       # 테스트 실행 서비스
    │   └── infrastructure/
    │       └── VirtualThreadEngine.java # Virtual Threads 구현
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
    │       └── JdkHttpClient.java      # JDK HttpClient 어댑터
    │
    ├── metric/                         # 메트릭 수집/계산
    │   ├── domain/
    │   │   ├── TestResult.java
    │   │   ├── Percentiles.java
    │   │   └── LatencyHistogram.java
    │   └── application/
    │       └── MetricAggregator.java
    │
    ├── config/                         # 설정 모델
    │   ├── LoadTestConfig.java
    │   └── HttpMethod.java
    │
    └── LoadTester.java                 # 메인 진입점 (Facade)
```

**사용 예시:**

```java
// 라이브러리로 사용
LoadTestConfig config = LoadTestConfig.builder()
    .url("https://api.example.com")
    .concurrency(100)
    .totalRequests(10000)
    .build();

LoadTestResult result = LoadTester.run(config);
```

---

### overload-cli (CLI 도구)

**역할:** 커맨드라인 인터페이스 제공

**의존성:** overload-core, picocli, jansi

```
overload-cli/
└── src/main/java/io/github/junhyeong9812/overload/cli/
    │
    ├── command/                        # CLI 명령어
    │   ├── RootCommand.java            # 루트 명령어
    │   ├── RunCommand.java             # run 서브 명령어
    │   └── ServeCommand.java           # serve 서브 명령어 (선택)
    │
    ├── config/                         # CLI 설정
    │   ├── YamlConfigLoader.java       # YAML 파일 파싱
    │   └── CliConfig.java
    │
    ├── output/                         # 출력 포매터
    │   ├── OutputFormatter.java
    │   ├── TextFormatter.java          # 터미널 텍스트 출력
    │   ├── JsonFormatter.java          # JSON 출력
    │   └── CsvFormatter.java           # CSV 출력
    │
    ├── progress/                       # 진행 상황 표시
    │   └── ProgressBar.java
    │
    └── Main.java                       # 진입점
```

**CLI 흐름:**

```
┌─────────────────────────────────────────────────────────┐
│  $ overload run -u https://... -c 100 -n 1000           │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  RunCommand.java (picocli)                              │
│  • 옵션 파싱                                             │
│  • YAML 파일 로드 (있으면)                               │
│  • LoadTestConfig 생성                                  │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  overload-core                                          │
│  LoadTester.run(config) → LoadTestResult                │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│  OutputFormatter                                        │
│  • TextFormatter → 터미널 출력                          │
│  • JsonFormatter → JSON 파일                            │
└─────────────────────────────────────────────────────────┘
```

---

### overload-web (웹 대시보드) - 선택적

**역할:** 웹 기반 대시보드, 실시간 모니터링

**의존성:** overload-core, Spring Boot

```
overload-web/
└── src/main/java/io/github/junhyeong9812/overload/web/
    │
    ├── dashboard/                      # 대시보드 기능
    │   ├── domain/
    │   ├── application/
    │   └── infrastructure/
    │       ├── web/                    # 화면 (Thymeleaf)
    │       ├── api/                    # REST API
    │       └── persistence/
    │
    └── config/
```

**Phase 3에서 구현 예정**

---

## 모듈 내부 구조: Hexagonal Architecture

각 모듈 내부에서는 **Feature-first + Hexagonal Architecture**를 적용합니다.

### 구조

```
feature/
├── domain/           # 핵심 비즈니스 로직 (순수 Java)
├── application/      # 유스케이스, 포트
└── infrastructure/   # 어댑터 (기술 구현체)
```

### 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                      Feature                            │
│                                                         │
│   Infrastructure        Application        Infrastructure
│   (Input Adapters)                         (Output Adapters)
│                                                         │
│   ┌───────────┐    ┌─────────────────┐    ┌───────────┐ │
│   │           │    │                 │    │           │ │
│   │   CLI     │───▶│   Port (in)     │    │ Port(out) │◀┼──┐
│   │  Command  │    │                 │    │           │ │  │
│   │           │    │  ┌───────────┐  │    └───────────┘ │  │
│   └───────────┘    │  │           │  │                  │  │
│                    │  │  Domain   │  │    ┌───────────┐ │  │
│   ┌───────────┐    │  │  (Core)   │  │    │           │ │  │
│   │           │    │  │           │  │    │  HTTP     │─┼──┘
│   │    API    │───▶│  └───────────┘  │    │  Client   │ │
│   │           │    │                 │    │           │ │
│   └───────────┘    │     Service     │    └───────────┘ │
│                    │                 │                  │
│                    └─────────────────┘                  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 의존성 규칙

```
infrastructure ──────▶ application ──────▶ domain
     │                      │                 │
     │                      │                 │
   Adapter              Port/Service      Pure Java
   (구현체)              (인터페이스)        (POJO)
```

**핵심 원칙:**

1. **Domain은 아무것도 의존하지 않음** - 순수 Java 코드
2. **의존성은 항상 안쪽을 향함** - infrastructure → application → domain
3. **Port는 인터페이스, Adapter는 구현체**

---

## 모듈 간 의존성

```
                    ┌──────────────┐
                    │ overload-cli │
                    └──────┬───────┘
                           │
                           │ 의존
                           ▼
┌──────────────┐    ┌──────────────┐
│ overload-web │───▶│overload-core │
└──────────────┘    └──────────────┘
        │                  │
        │ 의존             │ 의존 없음
        ▼                  ▼
   Spring Boot         JDK만
```

| 모듈 | 의존 대상 | 외부 의존성 |
|------|----------|------------|
| **overload-core** | 없음 | JDK만 |
| **overload-cli** | overload-core | picocli, jansi, snakeyaml |
| **overload-web** | overload-core | Spring Boot |

---

## Gradle 멀티 모듈 설정

### settings.gradle

```groovy
rootProject.name = 'overload'

include 'overload-core'
include 'overload-cli'
include 'overload-web'
```

### 루트 build.gradle

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
}
```

### overload-core/build.gradle

```groovy
plugins {
    id 'java-library'
}

description = 'Overload Core Engine - Pure Java Library'

dependencies {
    // 외부 의존성 없음 - JDK만 사용
    
    // 테스트
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.assertj:assertj-core:3.24.2'
}

test {
    useJUnitPlatform()
}
```

### overload-cli/build.gradle

```groovy
plugins {
    id 'application'
}

description = 'Overload CLI - Command Line Interface'

application {
    mainClass = 'io.github.junhyeong9812.overload.cli.Main'
}

dependencies {
    implementation project(':overload-core')
    
    // CLI
    implementation 'info.picocli:picocli:4.7.5'
    annotationProcessor 'info.picocli:picocli-codegen:4.7.5'
    
    // YAML 파싱
    implementation 'org.yaml:snakeyaml:2.2'
    
    // 터미널 색상
    implementation 'org.fusesource.jansi:jansi:2.4.1'
    
    // 테스트
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}

jar {
    manifest {
        attributes 'Main-Class': application.mainClass
    }
}
```

### overload-web/build.gradle

```groovy
plugins {
    id 'org.springframework.boot' version '4.0.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

description = 'Overload Web - Dashboard UI'

dependencies {
    implementation project(':overload-core')
    
    // Spring
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

---

## 전체 패키지 구조

```
overload/
│
├── overload-core/
│   └── src/main/java/io/github/junhyeong9812/overload/core/
│       │
│       ├── engine/                     # ⚡ 테스트 실행 엔진
│       │   ├── domain/
│       │   │   ├── LoadTestEngine.java
│       │   │   ├── ExecutionContext.java
│       │   │   └── LoadStrategy.java
│       │   ├── application/
│       │   │   └── TestExecutor.java
│       │   └── infrastructure/
│       │       └── VirtualThreadEngine.java
│       │
│       ├── http/                       # 🌐 HTTP 클라이언트
│       │   ├── domain/
│       │   │   ├── HttpRequest.java
│       │   │   ├── HttpResponse.java
│       │   │   └── RequestResult.java
│       │   ├── application/
│       │   │   └── port/
│       │   │       └── HttpClientPort.java
│       │   └── infrastructure/
│       │       └── JdkHttpClient.java
│       │
│       ├── metric/                     # 📊 메트릭
│       │   ├── domain/
│       │   │   ├── TestResult.java
│       │   │   ├── Percentiles.java
│       │   │   └── LatencyHistogram.java
│       │   └── application/
│       │       └── MetricAggregator.java
│       │
│       ├── config/                     # ⚙️ 설정
│       │   ├── LoadTestConfig.java
│       │   └── HttpMethod.java
│       │
│       └── LoadTester.java             # 🚀 Facade
│
├── overload-cli/
│   └── src/main/java/io/github/junhyeong9812/overload/cli/
│       │
│       ├── command/                    # 📟 CLI 명령어
│       │   ├── RootCommand.java
│       │   ├── RunCommand.java
│       │   └── ServeCommand.java
│       │
│       ├── config/                     # 📁 YAML 로더
│       │   └── YamlConfigLoader.java
│       │
│       ├── output/                     # 📤 출력
│       │   ├── OutputFormatter.java
│       │   ├── TextFormatter.java
│       │   ├── JsonFormatter.java
│       │   └── CsvFormatter.java
│       │
│       ├── progress/                   # 📈 진행률
│       │   └── ProgressBar.java
│       │
│       └── Main.java
│
├── overload-web/                       # (Phase 3)
│   └── src/main/java/io/github/junhyeong9812/overload/web/
│       └── ...
│
├── docs/
│   ├── architecture/
│   └── implementation/
│
├── build.gradle
└── settings.gradle
```

---

## 이 구조의 장점

### 1. 관심사 분리

| 모듈 | 관심사 |
|------|--------|
| core | 부하 테스트 엔진 로직 |
| cli | 커맨드라인 파싱, 출력 포매팅 |
| web | 웹 UI, HTTP 엔드포인트 |

### 2. 독립적 사용

```java
// overload-core만 의존성으로 추가하여 사용
LoadTestResult result = LoadTester.run(config);
```

### 3. 독립적 배포

```
overload-core-0.1.0.jar     # Maven Central에 라이브러리로 배포
overload-cli-0.1.0.tar.gz   # CLI 실행 파일로 배포
overload-web-0.1.0.jar      # 실행 가능한 Spring Boot JAR
```

### 4. 테스트 용이성

```java
// core만 단위 테스트
@Test
void shouldCalculatePercentiles() {
    List<Long> latencies = List.of(10L, 20L, 30L, 40L, 50L);
    Percentiles p = MetricAggregator.calculatePercentiles(latencies);
    assertThat(p.p50()).isEqualTo(30L);
}
```

---

## 참고 자료

- [Hexagonal Architecture - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [picocli - CLI Library](https://picocli.info/)
- [Get Your Hands Dirty on Clean Architecture - Tom Hombergs](https://www.packtpub.com/product/get-your-hands-dirty-on-clean-architecture)