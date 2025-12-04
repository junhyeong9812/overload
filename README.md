# Overload

**High-performance HTTP load testing tool powered by Java Virtual Threads**

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 소개

Overload는 Java 21/25의 **Virtual Threads**를 활용한 고성능 HTTP 부하 테스트 도구입니다.

기존의 Thread Pool 기반 테스트 도구들과 달리, Virtual Threads의 경량성을 활용하여 적은 리소스로 대규모 동시 요청을 생성할 수 있습니다.

### 왜 Overload인가?

| 기존 방식 | Overload |
|-----------|----------|
| OS Thread 기반 (1~4MB/thread) | Virtual Thread 기반 (~1KB/thread) |
| 수백 개 동시 요청 한계 | 수만 개 동시 요청 가능 |
| Reactive/Async 코드 복잡성 | 동기식 코드로 비동기 성능 |
| 무거운 설치 및 설정 | 단일 JAR 실행 |

---

## 주요 기능

- 🚀 **Virtual Threads 기반** - Java 21/25의 경량 스레드로 대규모 동시 요청
- 📊 **실시간 메트릭** - TPS, 응답시간, 성공률 실시간 모니터링
- 🎯 **다양한 테스트 시나리오** - 단일 URL, 시나리오 기반, Ramp-up 지원
- 📈 **상세 리포트** - Percentile(p50, p90, p95, p99), 히스토그램
- 🔌 **REST API** - 프로그래매틱 테스트 실행 및 CI/CD 연동
- ⚡ **경량 실행** - 단일 JAR, 최소 의존성

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.0 |
| Concurrency | Virtual Threads, Structured Concurrency |
| HTTP Client | Java HttpClient (JDK 내장) |
| Build | Gradle |
| Architecture | Feature-first + Hexagonal |

---

## 빠른 시작

### 요구사항

- Java 25+ (또는 Java 21+)
- Gradle 8.x

### 빌드

```bash
git clone https://github.com/junhyeong9812/overload.git
cd overload
./gradlew build
```

### 실행

```bash
java -jar build/libs/overload-0.0.1-SNAPSHOT.jar
```

### 간단한 테스트 실행

```bash
curl -X POST http://localhost:8080/api/v1/tests \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My First Test",
    "targetUrl": "https://httpbin.org/get",
    "method": "GET",
    "concurrentUsers": 100,
    "totalRequests": 1000,
    "timeout": 5000
  }'
```

---

## API 예시

### 테스트 생성 및 실행

```http
POST /api/v1/tests
Content-Type: application/json

{
  "name": "User API Load Test",
  "targetUrl": "https://api.example.com/users",
  "method": "GET",
  "concurrentUsers": 100,
  "totalRequests": 10000,
  "timeout": 5000,
  "headers": {
    "Authorization": "Bearer <token>"
  }
}
```

### 응답

```json
{
  "testId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "summary": {
    "totalRequests": 10000,
    "successCount": 9850,
    "failCount": 150,
    "avgResponseTime": 245.5,
    "minResponseTime": 12,
    "maxResponseTime": 1520,
    "requestsPerSecond": 892.3,
    "percentiles": {
      "p50": 180,
      "p90": 450,
      "p95": 680,
      "p99": 1200
    }
  }
}
```

---

## 프로젝트 구조

```
overload/
├── src/main/java/io/github/junhyeong9812/overload/
│   ├── loadtest/           # 테스트 관리 기능
│   │   ├── domain/
│   │   ├── application/
│   │   └── infrastructure/
│   ├── engine/             # 테스트 실행 엔진
│   │   ├── domain/
│   │   ├── application/
│   │   └── infrastructure/
│   ├── shared/             # 공통 모듈
│   └── config/             # 설정
├── docs/
│   ├── architecture/       # 아키텍처 문서
│   └── implementation/     # 구현 계획
└── build.gradle
```

> 아키텍처에 대한 자세한 내용은 [아키텍처 문서](docs/architecture/README.md)를 참고하세요.

---

## 문서

| 문서 | 설명 |
|------|------|
| [아키텍처 철학](docs/architecture/README.md) | Feature-first + Hexagonal 구조 설명 |
| [구현 계획](docs/implementation/README.md) | 단계별 구현 계획 및 상세 설계 |

---

## 로드맵

- [x] 프로젝트 초기 설정
- [ ] Phase 1: Core MVP
- [ ] Phase 2: Advanced Features
- [ ] Phase 3: UI & Monitoring

> 상세 로드맵은 [구현 계획](docs/implementation/README.md)을 참고하세요.

---

## 기여

기여는 언제나 환영합니다! Issue와 Pull Request를 통해 참여해주세요.

---

## 라이선스

MIT License - 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 연락처

- GitHub: [@junhyeong9812](https://github.com/junhyeong9812)