# overload-spring-boot-starter 구현 계획

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | overload-spring-boot-starter |
| Java 버전 | 21 |
| 주요 의존성 | Spring Boot 3.4, Thymeleaf, WebSocket |
| 아키텍처 | Spring 관례 (Controller → Service) |
| 목적 | @EnableOverload로 Web UI 대시보드 활성화 |

---

## 왜 Spring 관례를 따르는가?

Spring Boot Starter는 Spring 생태계의 일부입니다.

- `@EnableXxx`, `@AutoConfiguration` 패턴이 자연스러움
- Controller → Service 구조가 Spring 개발자에게 익숙함
- 복잡한 로직은 Core에 위임하므로 얇은 레이어
- Hexagonal은 오버 엔지니어링

---

## 폴더 구조

```
overload-spring-boot-starter/
├── build.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   └── io/github/junhyeong9812/overload/starter/
    │   │       │
    │   │       ├── EnableOverload.java             # @EnableOverload 어노테이션
    │   │       ├── OverloadAutoConfiguration.java  # Auto-configuration
    │   │       ├── OverloadProperties.java         # 설정 프로퍼티
    │   │       │
    │   │       ├── controller/                     # 웹 레이어
    │   │       │   ├── OverloadDashboardController.java
    │   │       │   └── OverloadApiController.java
    │   │       │
    │   │       ├── service/                        # 서비스 레이어
    │   │       │   ├── LoadTestService.java
    │   │       │   └── ResultBroadcastService.java
    │   │       │
    │   │       ├── dto/                            # 데이터 전송 객체
    │   │       │   ├── TestRequest.java
    │   │       │   ├── TestResponse.java
    │   │       │   └── ProgressMessage.java
    │   │       │
    │   │       ├── websocket/                      # WebSocket
    │   │       │   ├── WebSocketConfig.java
    │   │       │   └── OverloadWebSocketHandler.java
    │   │       │
    │   │       └── exception/                      # 예외 처리
    │   │           └── GlobalExceptionHandler.java
    │   │
    │   └── resources/
    │       ├── META-INF/
    │       │   └── spring/
    │       │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │       │
    │       ├── templates/
    │       │   └── overload/
    │       │       └── dashboard.html
    │       │
    │       └── static/
    │           └── overload/
    │               ├── css/
    │               │   └── dashboard.css
    │               └── js/
    │                   └── dashboard.js
    │
    └── test/
        └── java/
            └── io/github/junhyeong9812/overload/starter/
```

---

## 의존성

### build.gradle

```groovy
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.4.1' apply false
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencyManagement {
    imports {
        mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
    }
}

dependencies {
    // Core 모듈
    api project(':overload-core')
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Optional: Security
    compileOnly 'org.springframework.boot:spring-boot-starter-security'
    
    // Configuration Processor
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // 테스트
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// Spring Boot 플러그인 적용하지 않음 (라이브러리이므로)
// bootJar 태스크 비활성화
tasks.named('bootJar') {
    enabled = false
}

tasks.named('jar') {
    enabled = true
}
```

---

## 구현 상세

### 1. EnableOverload.java

```java
package io.github.junhyeong9812.overload.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Overload 부하 테스트 대시보드를 활성화합니다.
 * 
 * <pre>
 * {@code
 * @SpringBootApplication
 * @EnableOverload
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }
 * </pre>
 * 
 * 활성화 후 {@code /overload} 경로로 대시보드에 접근할 수 있습니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(OverloadAutoConfiguration.class)
public @interface EnableOverload {
}
```

### 2. OverloadAutoConfiguration.java

```java
package io.github.junhyeong9812.overload.starter;

import io.github.junhyeong9812.overload.starter.controller.OverloadApiController;
import io.github.junhyeong9812.overload.starter.controller.OverloadDashboardController;
import io.github.junhyeong9812.overload.starter.service.LoadTestService;
import io.github.junhyeong9812.overload.starter.service.ResultBroadcastService;
import io.github.junhyeong9812.overload.starter.websocket.OverloadWebSocketHandler;
import io.github.junhyeong9812.overload.starter.websocket.WebSocketConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(OverloadProperties.class)
@ConditionalOnProperty(
    prefix = "overload",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Import(WebSocketConfig.class)
public class OverloadAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public LoadTestService loadTestService(
            OverloadProperties properties,
            ResultBroadcastService broadcastService) {
        return new LoadTestService(properties, broadcastService);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ResultBroadcastService resultBroadcastService() {
        return new ResultBroadcastService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OverloadWebSocketHandler overloadWebSocketHandler(
            ResultBroadcastService broadcastService) {
        return new OverloadWebSocketHandler(broadcastService);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OverloadDashboardController overloadDashboardController(
            OverloadProperties properties) {
        return new OverloadDashboardController(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OverloadApiController overloadApiController(
            LoadTestService loadTestService) {
        return new OverloadApiController(loadTestService);
    }
}
```

### 3. OverloadProperties.java

```java
package io.github.junhyeong9812.overload.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "overload")
public class OverloadProperties {
    
    /**
     * Overload 활성화 여부
     */
    private boolean enabled = true;
    
    /**
     * 대시보드 설정
     */
    private Dashboard dashboard = new Dashboard();
    
    /**
     * 기본값 설정
     */
    private Defaults defaults = new Defaults();
    
    /**
     * 보안 설정
     */
    private Security security = new Security();
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Dashboard getDashboard() {
        return dashboard;
    }
    
    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }
    
    public Defaults getDefaults() {
        return defaults;
    }
    
    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }
    
    public Security getSecurity() {
        return security;
    }
    
    public void setSecurity(Security security) {
        this.security = security;
    }
    
    public static class Dashboard {
        private String path = "/overload";
        private String title = "Overload - Load Test Dashboard";
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }
    
    public static class Defaults {
        private int concurrency = 10;
        private int requests = 100;
        private Duration timeout = Duration.ofSeconds(30);
        
        public int getConcurrency() {
            return concurrency;
        }
        
        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }
        
        public int getRequests() {
            return requests;
        }
        
        public void setRequests(int requests) {
            this.requests = requests;
        }
        
        public Duration getTimeout() {
            return timeout;
        }
        
        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class Security {
        private boolean enabled = false;
        private String username = "admin";
        private String password = "admin";
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
```

---

### 4. controller 패키지

#### OverloadDashboardController.java

```java
package io.github.junhyeong9812.overload.starter.controller;

import io.github.junhyeong9812.overload.starter.OverloadProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OverloadDashboardController {
    
    private final OverloadProperties properties;
    
    public OverloadDashboardController(OverloadProperties properties) {
        this.properties = properties;
    }
    
    @GetMapping("${overload.dashboard.path:/overload}")
    public String dashboard(Model model) {
        model.addAttribute("title", properties.getDashboard().getTitle());
        model.addAttribute("defaults", properties.getDefaults());
        return "overload/dashboard";
    }
}
```

#### OverloadApiController.java

```java
package io.github.junhyeong9812.overload.starter.controller;

import io.github.junhyeong9812.overload.starter.dto.TestRequest;
import io.github.junhyeong9812.overload.starter.dto.TestResponse;
import io.github.junhyeong9812.overload.starter.service.LoadTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${overload.dashboard.path:/overload}/api")
public class OverloadApiController {
    
    private final LoadTestService loadTestService;
    
    public OverloadApiController(LoadTestService loadTestService) {
        this.loadTestService = loadTestService;
    }
    
    /**
     * 부하 테스트 시작
     */
    @PostMapping("/tests")
    public ResponseEntity<TestResponse> startTest(@Valid @RequestBody TestRequest request) {
        String testId = UUID.randomUUID().toString();
        loadTestService.startTest(testId, request);
        return ResponseEntity.ok(new TestResponse(testId, "RUNNING"));
    }
    
    /**
     * 테스트 상태 조회
     */
    @GetMapping("/tests/{testId}")
    public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable String testId) {
        return ResponseEntity.ok(loadTestService.getTestStatus(testId));
    }
    
    /**
     * 테스트 중지
     */
    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Void> stopTest(@PathVariable String testId) {
        loadTestService.stopTest(testId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 최근 테스트 목록
     */
    @GetMapping("/tests")
    public ResponseEntity<Object> getRecentTests(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(loadTestService.getRecentTests(limit));
    }
}
```

---

### 5. service 패키지

#### LoadTestService.java

```java
package io.github.junhyeong9812.overload.starter.service;

import io.github.junhyeong9812.overload.core.LoadTester;
import io.github.junhyeong9812.overload.core.config.HttpMethod;
import io.github.junhyeong9812.overload.core.config.LoadTestConfig;
import io.github.junhyeong9812.overload.core.metric.domain.TestResult;
import io.github.junhyeong9812.overload.starter.OverloadProperties;
import io.github.junhyeong9812.overload.starter.dto.ProgressMessage;
import io.github.junhyeong9812.overload.starter.dto.TestRequest;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadTestService {
    
    private final OverloadProperties properties;
    private final ResultBroadcastService broadcastService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    private final Map<String, TestStatus> runningTests = new ConcurrentHashMap<>();
    private final List<TestHistory> testHistory = Collections.synchronizedList(new ArrayList<>());
    
    public LoadTestService(
            OverloadProperties properties, 
            ResultBroadcastService broadcastService) {
        this.properties = properties;
        this.broadcastService = broadcastService;
    }
    
    public void startTest(String testId, TestRequest request) {
        LoadTestConfig config = buildConfig(request);
        
        TestStatus status = new TestStatus(testId, "RUNNING", 0, request.totalRequests());
        runningTests.put(testId, status);
        
        executor.submit(() -> {
            try {
                TestResult result = LoadTester.run(config, (completed, total) -> {
                    status.setCompleted(completed);
                    broadcastService.broadcast(new ProgressMessage(
                        testId, completed, total, "RUNNING"
                    ));
                });
                
                status.setStatus("COMPLETED");
                status.setResult(result);
                
                // 이력 저장
                testHistory.add(0, new TestHistory(testId, request, result));
                if (testHistory.size() > 100) {
                    testHistory.remove(testHistory.size() - 1);
                }
                
                broadcastService.broadcast(new ProgressMessage(
                    testId, config.totalRequests(), config.totalRequests(), "COMPLETED"
                ));
                
            } catch (Exception e) {
                status.setStatus("FAILED");
                status.setError(e.getMessage());
                
                broadcastService.broadcast(new ProgressMessage(
                    testId, status.getCompleted(), status.getTotal(), "FAILED"
                ));
            } finally {
                runningTests.remove(testId);
            }
        });
    }
    
    public Map<String, Object> getTestStatus(String testId) {
        TestStatus status = runningTests.get(testId);
        if (status == null) {
            return Map.of("status", "NOT_FOUND");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("testId", testId);
        response.put("status", status.getStatus());
        response.put("completed", status.getCompleted());
        response.put("total", status.getTotal());
        
        if (status.getResult() != null) {
            response.put("result", status.getResult());
        }
        if (status.getError() != null) {
            response.put("error", status.getError());
        }
        
        return response;
    }
    
    public void stopTest(String testId) {
        TestStatus status = runningTests.get(testId);
        if (status != null) {
            status.setStatus("CANCELLED");
        }
    }
    
    public List<TestHistory> getRecentTests(int limit) {
        return testHistory.stream()
            .limit(limit)
            .toList();
    }
    
    private LoadTestConfig buildConfig(TestRequest request) {
        var defaults = properties.getDefaults();
        
        return LoadTestConfig.builder()
            .url(request.url())
            .method(HttpMethod.valueOf(request.method().toUpperCase()))
            .headers(request.headers() != null ? request.headers() : Map.of())
            .body(request.body())
            .concurrency(request.concurrency() > 0 ? request.concurrency() : defaults.getConcurrency())
            .totalRequests(request.totalRequests() > 0 ? request.totalRequests() : defaults.getRequests())
            .timeout(request.timeoutMs() > 0 
                ? Duration.ofMillis(request.timeoutMs()) 
                : defaults.getTimeout())
            .build();
    }
    
    // Inner classes
    
    private static class TestStatus {
        private final String testId;
        private String status;
        private int completed;
        private final int total;
        private TestResult result;
        private String error;
        
        public TestStatus(String testId, String status, int completed, int total) {
            this.testId = testId;
            this.status = status;
            this.completed = completed;
            this.total = total;
        }
        
        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getCompleted() { return completed; }
        public void setCompleted(int completed) { this.completed = completed; }
        public int getTotal() { return total; }
        public TestResult getResult() { return result; }
        public void setResult(TestResult result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public record TestHistory(String testId, TestRequest request, TestResult result) {}
}
```

#### ResultBroadcastService.java

```java
package io.github.junhyeong9812.overload.starter.service;

import io.github.junhyeong9812.overload.starter.dto.ProgressMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ResultBroadcastService {
    
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    
    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }
    
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }
    
    public void broadcast(ProgressMessage message) {
        String json = toJson(message);
        TextMessage textMessage = new TextMessage(json);
        
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException e) {
                // 전송 실패 무시
            }
        });
    }
    
    private String toJson(ProgressMessage message) {
        return String.format(
            "{\"testId\":\"%s\",\"completed\":%d,\"total\":%d,\"status\":\"%s\"}",
            message.testId(),
            message.completed(),
            message.total(),
            message.status()
        );
    }
}
```

---

### 6. dto 패키지

#### TestRequest.java

```java
package io.github.junhyeong9812.overload.starter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Map;

public record TestRequest(
    @NotBlank(message = "URL is required")
    String url,
    
    String method,
    
    Map<String, String> headers,
    
    String body,
    
    @Positive(message = "Concurrency must be positive")
    int concurrency,
    
    @Positive(message = "Total requests must be positive")
    int totalRequests,
    
    int timeoutMs
) {
    public TestRequest {
        if (method == null || method.isBlank()) {
            method = "GET";
        }
    }
}
```

#### TestResponse.java

```java
package io.github.junhyeong9812.overload.starter.dto;

public record TestResponse(
    String testId,
    String status
) {}
```

#### ProgressMessage.java

```java
package io.github.junhyeong9812.overload.starter.dto;

public record ProgressMessage(
    String testId,
    int completed,
    int total,
    String status
) {
    public double percentage() {
        return total > 0 ? (double) completed / total * 100 : 0;
    }
}
```

---

### 7. websocket 패키지

#### WebSocketConfig.java

```java
package io.github.junhyeong9812.overload.starter.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final OverloadWebSocketHandler handler;
    private final String basePath;
    
    public WebSocketConfig(
            OverloadWebSocketHandler handler,
            @Value("${overload.dashboard.path:/overload}") String basePath) {
        this.handler = handler;
        this.basePath = basePath;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, basePath + "/ws")
            .setAllowedOrigins("*");
    }
}
```

#### OverloadWebSocketHandler.java

```java
package io.github.junhyeong9812.overload.starter.websocket;

import io.github.junhyeong9812.overload.starter.service.ResultBroadcastService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class OverloadWebSocketHandler extends TextWebSocketHandler {
    
    private final ResultBroadcastService broadcastService;
    
    public OverloadWebSocketHandler(ResultBroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcastService.addSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcastService.removeSession(session);
    }
}
```

---

### 8. 리소스 파일

#### META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

```
io.github.junhyeong9812.overload.starter.OverloadAutoConfiguration
```

#### templates/overload/dashboard.html

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">Overload - Load Test Dashboard</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 min-h-screen">
    <div class="container mx-auto px-4 py-8">
        <h1 class="text-3xl font-bold text-gray-800 mb-8" th:text="${title}">
            🚀 Overload - Load Test Dashboard
        </h1>
        
        <!-- Request Configuration -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
            <h2 class="text-xl font-semibold mb-4">📍 Target URL</h2>
            <input type="text" id="url" 
                   class="w-full p-3 border rounded-lg"
                   placeholder="https://api.example.com/users">
            
            <h2 class="text-xl font-semibold mt-6 mb-4">📋 Request Configuration</h2>
            <div class="grid grid-cols-2 gap-4">
                <div>
                    <label class="block text-sm font-medium mb-2">Method</label>
                    <select id="method" class="w-full p-3 border rounded-lg">
                        <option value="GET">GET</option>
                        <option value="POST">POST</option>
                        <option value="PUT">PUT</option>
                        <option value="DELETE">DELETE</option>
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium mb-2">Concurrency</label>
                    <input type="number" id="concurrency" 
                           class="w-full p-3 border rounded-lg"
                           th:value="${defaults.concurrency}">
                </div>
            </div>
            
            <div class="mt-4">
                <label class="block text-sm font-medium mb-2">Total Requests</label>
                <input type="number" id="totalRequests" 
                       class="w-full p-3 border rounded-lg"
                       th:value="${defaults.requests}">
            </div>
            
            <div class="mt-4">
                <label class="block text-sm font-medium mb-2">Headers (JSON)</label>
                <textarea id="headers" 
                          class="w-full p-3 border rounded-lg h-20"
                          placeholder='{"Authorization": "Bearer xxx"}'></textarea>
            </div>
            
            <div class="mt-4">
                <label class="block text-sm font-medium mb-2">Body</label>
                <textarea id="body" 
                          class="w-full p-3 border rounded-lg h-24"
                          placeholder='{"name": "test"}'></textarea>
            </div>
        </div>
        
        <!-- Controls -->
        <div class="flex gap-4 mb-6">
            <button onclick="startTest()" 
                    class="bg-green-500 text-white px-6 py-3 rounded-lg font-semibold hover:bg-green-600">
                ▶ Run Load Test
            </button>
            <button onclick="stopTest()" 
                    class="bg-red-500 text-white px-6 py-3 rounded-lg font-semibold hover:bg-red-600">
                ⬛ Stop
            </button>
        </div>
        
        <!-- Progress -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
            <h2 class="text-xl font-semibold mb-4">📊 Real-time Results</h2>
            <div class="mb-4">
                <div class="flex justify-between mb-2">
                    <span id="progressText">0%</span>
                    <span id="progressCount">0/0</span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-4">
                    <div id="progressBar" 
                         class="bg-green-500 h-4 rounded-full transition-all duration-300"
                         style="width: 0%"></div>
                </div>
            </div>
            
            <!-- Results -->
            <div id="results" class="hidden">
                <div class="grid grid-cols-3 gap-4 mt-6">
                    <div class="bg-gray-50 p-4 rounded-lg">
                        <div class="text-sm text-gray-500">RPS</div>
                        <div id="rps" class="text-2xl font-bold">-</div>
                    </div>
                    <div class="bg-gray-50 p-4 rounded-lg">
                        <div class="text-sm text-gray-500">Avg Latency</div>
                        <div id="avgLatency" class="text-2xl font-bold">-</div>
                    </div>
                    <div class="bg-gray-50 p-4 rounded-lg">
                        <div class="text-sm text-gray-500">Success Rate</div>
                        <div id="successRate" class="text-2xl font-bold">-</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    
    <script th:inline="javascript">
        let currentTestId = null;
        let ws = null;
        
        function connectWebSocket() {
            const basePath = /*[[${@environment.getProperty('overload.dashboard.path', '/overload')}]]*/ '/overload';
            ws = new WebSocket(`ws://${window.location.host}${basePath}/ws`);
            
            ws.onmessage = function(event) {
                const data = JSON.parse(event.data);
                updateProgress(data);
            };
            
            ws.onclose = function() {
                setTimeout(connectWebSocket, 3000);
            };
        }
        
        function startTest() {
            const request = {
                url: document.getElementById('url').value,
                method: document.getElementById('method').value,
                concurrency: parseInt(document.getElementById('concurrency').value),
                totalRequests: parseInt(document.getElementById('totalRequests').value),
                headers: parseJson(document.getElementById('headers').value),
                body: document.getElementById('body').value
            };
            
            const basePath = /*[[${@environment.getProperty('overload.dashboard.path', '/overload')}]]*/ '/overload';
            
            fetch(`${basePath}/api/tests`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(request)
            })
            .then(res => res.json())
            .then(data => {
                currentTestId = data.testId;
                document.getElementById('results').classList.add('hidden');
            });
        }
        
        function stopTest() {
            if (currentTestId) {
                const basePath = /*[[${@environment.getProperty('overload.dashboard.path', '/overload')}]]*/ '/overload';
                fetch(`${basePath}/api/tests/${currentTestId}`, {method: 'DELETE'});
            }
        }
        
        function updateProgress(data) {
            if (data.testId !== currentTestId) return;
            
            const percent = (data.completed / data.total * 100).toFixed(1);
            document.getElementById('progressBar').style.width = percent + '%';
            document.getElementById('progressText').textContent = percent + '%';
            document.getElementById('progressCount').textContent = 
                `${data.completed.toLocaleString()}/${data.total.toLocaleString()}`;
            
            if (data.status === 'COMPLETED') {
                fetchResults();
            }
        }
        
        function fetchResults() {
            const basePath = /*[[${@environment.getProperty('overload.dashboard.path', '/overload')}]]*/ '/overload';
            
            fetch(`${basePath}/api/tests/${currentTestId}`)
                .then(res => res.json())
                .then(data => {
                    if (data.result) {
                        document.getElementById('rps').textContent = 
                            data.result.requestsPerSecond.toFixed(2);
                        document.getElementById('avgLatency').textContent = 
                            data.result.latencyStats.avg.toFixed(0) + 'ms';
                        document.getElementById('successRate').textContent = 
                            data.result.successRate.toFixed(1) + '%';
                        document.getElementById('results').classList.remove('hidden');
                    }
                });
        }
        
        function parseJson(str) {
            try {
                return str ? JSON.parse(str) : {};
            } catch {
                return {};
            }
        }
        
        connectWebSocket();
    </script>
</body>
</html>
```

---

## REST API 명세

| 메서드 | 경로 | 설명 | 요청 | 응답 |
|--------|------|------|------|------|
| GET | /overload | 대시보드 UI | - | HTML |
| POST | /overload/api/tests | 테스트 시작 | TestRequest | TestResponse |
| GET | /overload/api/tests/{id} | 테스트 상태 | - | 상태 JSON |
| DELETE | /overload/api/tests/{id} | 테스트 중지 | - | 204 |
| GET | /overload/api/tests | 테스트 이력 | ?limit=10 | 목록 JSON |
| WS | /overload/ws | 실시간 결과 | - | ProgressMessage |

---

## 구현 체크리스트

- [ ] EnableOverload.java
- [ ] OverloadAutoConfiguration.java
- [ ] OverloadProperties.java
- [ ] controller 패키지
    - [ ] OverloadDashboardController
    - [ ] OverloadApiController
- [ ] service 패키지
    - [ ] LoadTestService
    - [ ] ResultBroadcastService
- [ ] dto 패키지
    - [ ] TestRequest
    - [ ] TestResponse
    - [ ] ProgressMessage
- [ ] websocket 패키지
    - [ ] WebSocketConfig
    - [ ] OverloadWebSocketHandler
- [ ] resources
    - [ ] AutoConfiguration.imports
    - [ ] dashboard.html
- [ ] 테스트