package io.github.junhyeong9812.overload.starter.controller;

import io.github.junhyeong9812.overload.starter.dto.TestRequest;
import io.github.junhyeong9812.overload.starter.dto.TestResponse;
import io.github.junhyeong9812.overload.starter.service.LoadTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 부하 테스트 REST API 컨트롤러.
 *
 * <p>부하 테스트의 시작, 중지, 상태 조회 등의 REST API를 제공한다.
 * 모든 엔드포인트는 {@code ${overload.dashboard.path}/api} 경로 아래에 매핑된다.
 *
 * <p><b>엔드포인트:</b>
 * <ul>
 *   <li>{@code POST /api/tests} - 새 테스트 시작</li>
 *   <li>{@code GET /api/tests/{testId}} - 테스트 상태 조회</li>
 *   <li>{@code DELETE /api/tests/{testId}} - 테스트 중지</li>
 *   <li>{@code GET /api/tests} - 최근 테스트 이력 조회</li>
 *   <li>{@code GET /api/tests/active} - 활성 테스트 목록 조회</li>
 * </ul>
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@RestController
@RequestMapping("${overload.dashboard.path:/overload}/api")
public class OverloadApiController {

  private final LoadTestService loadTestService;

  /**
   * OverloadApiController를 생성한다.
   *
   * @param loadTestService 부하 테스트 서비스
   */
  public OverloadApiController(LoadTestService loadTestService) {
    this.loadTestService = loadTestService;
  }

  /**
   * 새로운 부하 테스트를 시작한다.
   *
   * <p>고유한 테스트 ID가 생성되어 반환되며, 테스트는 비동기로 실행된다.
   *
   * @param request 테스트 요청 정보
   * @return 테스트 ID와 상태를 포함한 응답
   */
  @PostMapping("/tests")
  public ResponseEntity<TestResponse> startTest(@Valid @RequestBody TestRequest request) {
    String testId = UUID.randomUUID().toString().substring(0, 8);
    loadTestService.startTest(testId, request);
    return ResponseEntity.ok(new TestResponse(testId, "RUNNING"));
  }

  /**
   * 테스트 상태를 조회한다.
   *
   * <p>테스트 진행 상황, 결과, 최근 요청 로그 등을 반환한다.
   *
   * @param testId 테스트 ID
   * @return 테스트 상태 정보
   */
  @GetMapping("/tests/{testId}")
  public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable("testId") String testId) {
    return ResponseEntity.ok(loadTestService.getTestStatus(testId));
  }

  /**
   * 실행 중인 테스트를 중지한다.
   *
   * @param testId 테스트 ID
   * @return 204 No Content
   */
  @DeleteMapping("/tests/{testId}")
  public ResponseEntity<Void> stopTest(@PathVariable("testId") String testId) {
    loadTestService.stopTest(testId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 최근 완료된 테스트 이력을 조회한다.
   *
   * @param limit 조회할 최대 개수 (기본값: 10)
   * @return 테스트 이력 목록
   */
  @GetMapping("/tests")
  public ResponseEntity<List<Map<String, Object>>> getRecentTests(
      @RequestParam(value = "limit", defaultValue = "10") int limit) {
    return ResponseEntity.ok(loadTestService.getRecentTests(limit));
  }

  /**
   * 현재 활성 상태인 모든 테스트를 조회한다.
   *
   * <p>실행 중이거나 최근 완료된 테스트 목록을 반환한다.
   *
   * @return 활성 테스트 목록
   */
  @GetMapping("/tests/active")
  public ResponseEntity<List<Map<String, Object>>> getActiveTests() {
    return ResponseEntity.ok(loadTestService.getAllTests());
  }
}