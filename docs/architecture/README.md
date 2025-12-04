# 아키텍처 철학: Feature-first + Hexagonal

이 문서는 Overload 프로젝트에서 채택한 아키텍처 구조의 철학과 원칙을 설명합니다.

---

## 개요

Overload는 **Feature-first** 패키지 구조와 **Hexagonal Architecture**를 결합한 방식을 채택합니다.

```
feature/
├── domain/
├── application/
└── infrastructure/
```

이 구조는 "코드를 어떻게 기술적으로 분류할 것인가"가 아닌 **"이 시스템이 무엇을 하는가"**를 먼저 보여줍니다.

---

## Package by Layer vs Package by Feature

### Package by Layer (전통적 방식)

```
├── controller/
│   ├── LoadTestController.java
│   └── EngineController.java
├── service/
│   ├── LoadTestService.java
│   └── EngineService.java
├── repository/
│   ├── LoadTestRepository.java
│   └── EngineRepository.java
```

**문제점:**

- 패키지 구조만 보면 "컨트롤러, 서비스, 리포지토리가 있구나"만 알 수 있음
- 하나의 기능을 이해하려면 여러 패키지를 돌아다녀야 함
- 패키지 간 결합도가 높고, 패키지 내 응집도가 낮음
- 거의 모든 클래스가 `public`이어야 함
- 프로젝트가 커지면 각 레이어 패키지의 클래스 수가 무한정 증가

### Package by Feature (기능 우선)

```
├── loadtest/
│   ├── LoadTestController.java
│   ├── LoadTestService.java
│   └── LoadTestRepository.java
├── engine/
│   ├── EngineController.java
│   ├── EngineService.java
│   └── EngineRepository.java
```

**장점:**

- 패키지 구조만 보면 "로드테스트, 엔진 기능이 있구나"를 바로 알 수 있음
- 하나의 기능에 필요한 모든 코드가 한 곳에 모여 있음
- 기능 삭제 시 해당 패키지만 삭제하면 됨
- `package-private` 접근 제어자 활용 가능 (캡슐화 향상)
- 마이크로서비스 전환 시 기능 단위로 분리 용이

---

## Screaming Architecture

> "아키텍처는 프레임워크가 아니라 유스케이스를 소리쳐야 한다."
> — Robert C. Martin (Uncle Bob)

프로젝트의 최상위 패키지 구조를 보면:

```
├── loadtest/      ← "이 시스템은 로드 테스트를 관리한다"
├── engine/        ← "이 시스템은 테스트 엔진을 가지고 있다"
└── shared/        ← "공통 유틸리티가 있다"
```

Spring, JPA, Web 같은 **기술적 용어가 아닌 비즈니스 도메인**이 보입니다. 이것이 "Screaming Architecture"의 핵심입니다. 코드 구조 자체가 이 애플리케이션이 무엇을 하는지 소리치고 있습니다.

---

## Hexagonal Architecture (Ports & Adapters)

Feature 내부는 Hexagonal Architecture를 따릅니다.

### 구조 다이어그램

```
┌─────────────────────────────────────────────────────────┐
│                      Feature                            │
│                                                         │
│   Infrastructure        Application         Infrastructure
│   (Input Adapters)                          (Output Adapters)
│                                                         │
│   ┌───────────┐    ┌─────────────────┐    ┌───────────┐ │
│   │           │    │                 │    │           │ │
│   │    Web    │───▶│   Port (in)     │    │  Port(out)│◀┼──┐
│   │ Controller│    │                 │    │           │ │  │
│   │           │    │  ┌───────────┐  │    └───────────┘ │  │
│   └───────────┘    │  │           │  │                  │  │
│                    │  │  Domain   │  │    ┌───────────┐ │  │
│   ┌───────────┐    │  │  (Core)   │  │    │           │ │  │
│   │           │    │  │           │  │    │  Database │─┼──┘
│   │    CLI    │───▶│  └───────────┘  │    │  Adapter  │ │
│   │           │    │                 │    │           │ │
│   └───────────┘    │     Service     │    └───────────┘ │
│                    │                 │                  │
│                    └─────────────────┘    ┌───────────┐ │
│                                           │  External │ │
│                                           │   HTTP    │ │
│                                           │  Client   │ │
│                                           └───────────┘ │
└─────────────────────────────────────────────────────────┘
```

### 핵심 원칙

1. **Domain은 아무것도 의존하지 않음**
    - 순수 Java 코드
    - 프레임워크, 라이브러리 의존성 없음
    - 비즈니스 로직만 포함

2. **의존성은 항상 안쪽을 향함**
   ```
   Infrastructure → Application → Domain
   ```

3. **Port는 인터페이스, Adapter는 구현체**
    - Input Port: 외부에서 도메인으로 들어오는 진입점 (UseCase)
    - Output Port: 도메인에서 외부로 나가는 출구 (Repository, Client)
    - Adapter: Port의 실제 구현

4. **외부 세계와의 연결은 오직 Adapter를 통해서만**
    - 데이터베이스 변경? → Adapter만 교체
    - REST → gRPC 변경? → Adapter만 교체
    - 도메인 로직은 영향 없음

---

## 실제 패키지 구조

```
io.github.junhyeong9812.overload/
│
├── loadtest/                              # 📋 테스트 관리 기능
│   │
│   ├── domain/                            # 핵심 도메인 (순수 Java)
│   │   ├── LoadTest.java                  # 루트 엔티티
│   │   ├── TestConfig.java                # 설정 Value Object
│   │   ├── TestResult.java                # 결과 Value Object
│   │   └── TestStatus.java                # 상태 Enum
│   │
│   ├── application/                       # 유스케이스 레이어
│   │   ├── port/
│   │   │   ├── in/                        # Input Ports (Driving)
│   │   │   │   ├── ExecuteLoadTestUseCase.java
│   │   │   │   └── GetTestResultUseCase.java
│   │   │   └── out/                       # Output Ports (Driven)
│   │   │       └── LoadTestRepositoryPort.java
│   │   └── service/
│   │       └── LoadTestService.java       # UseCase 구현체
│   │
│   └── infrastructure/                    # 어댑터 레이어
│       ├── web/                           # Input Adapter
│       │   ├── LoadTestController.java
│       │   └── dto/
│       │       ├── LoadTestRequest.java
│       │       └── LoadTestResponse.java
│       └── persistence/                   # Output Adapter
│           └── InMemoryLoadTestRepository.java
│
├── engine/                                # ⚡ 테스트 실행 엔진
│   │
│   ├── domain/
│   │   ├── TestEngine.java                # 엔진 인터페이스
│   │   ├── ExecutionContext.java          # 실행 컨텍스트
│   │   └── vo/
│   │       ├── RequestResult.java
│   │       └── Percentile.java
│   │
│   ├── application/
│   │   ├── port/
│   │   │   └── out/
│   │   │       └── HttpRequestPort.java   # HTTP 요청 추상화
│   │   └── service/
│   │       └── TestExecutionService.java
│   │
│   └── infrastructure/
│       ├── executor/                      # Virtual Threads 구현
│       │   └── VirtualThreadTestEngine.java
│       └── http/
│           └── JavaHttpClientAdapter.java # JDK HttpClient 사용
│
├── shared/                                # 🔧 공통 모듈
│   ├── util/
│   │   └── TimeUtils.java
│   └── exception/
│       └── OverloadException.java
│
└── config/                                # ⚙️ 설정
    └── BeanConfig.java
```

---

## 의존성 규칙

### Feature 내부 의존성

```
infrastructure ──────▶ application ──────▶ domain
     │                      │                 │
     │                      │                 │
   Adapter              Port/Service      Pure Java
   (구현체)              (인터페이스)        (POJO)
```

### Feature 간 의존성

```
loadtest ───────────────▶ engine
    │                        │
    │                        │
    ▼                        ▼
 shared ◀─────────────── shared
```

- `loadtest`는 `engine`을 사용 (테스트 실행 위임)
- 두 기능 모두 `shared`의 공통 유틸 사용 가능
- `engine`은 `loadtest`를 알지 못함 (단방향 의존)

---

## 이 구조의 장점

### 1. 테스트 용이성

```java
// HttpRequestPort를 Mock으로 대체하여 실제 HTTP 호출 없이 테스트
@Test
void shouldExecuteLoadTest() {
    HttpRequestPort mockPort = mock(HttpRequestPort.class);
    when(mockPort.send(any())).thenReturn(successResult());
    
    // 테스트 실행
}
```

### 2. 기술 교체 용이성

```
현재: InMemoryLoadTestRepository
미래: JpaLoadTestRepository 또는 MongoLoadTestRepository

→ Port 인터페이스는 그대로, Adapter만 교체
→ Domain, Application 코드 변경 없음
```

### 3. 명확한 경계

```java
// Domain에서는 절대로 이런 코드가 나오면 안 됨
import org.springframework.web.bind.annotation.*;  // ❌
import jakarta.persistence.*;                      // ❌

// Domain은 순수 Java만
public class LoadTest {
    private final LoadTestId id;
    private final TestConfig config;
    // ...
}
```

### 4. 기능 단위 개발/삭제

```
새 기능 추가: reporting/ 폴더 생성 후 domain, application, infrastructure 구현
기능 삭제: 해당 폴더 삭제
```

---

## 참고 자료

- [Hexagonal Architecture - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Get Your Hands Dirty on Clean Architecture - Tom Hombergs](https://www.packtpub.com/product/get-your-hands-dirty-on-clean-architecture)
- [Package by Feature - Philipp Hauer](https://phauer.com/2020/package-by-feature/)
- [DDD, Hexagonal, Onion, Clean, CQRS - Herberto Graca](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)