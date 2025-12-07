package io.github.junhyeong9812.overload.starter.scenario.controller;

import io.github.junhyeong9812.overload.starter.scenario.dto.ScenarioRequest;
import io.github.junhyeong9812.overload.starter.scenario.dto.ScenarioResponse;
import io.github.junhyeong9812.overload.starter.scenario.service.ScenarioTestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 시나리오 테스트 REST API 컨트롤러.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@RestController
@RequestMapping("${overload.dashboard.path:/overload}/api/scenarios")
public class ScenarioApiController {

  private final ScenarioTestService scenarioTestService;

  /**
   * ScenarioApiController를 생성한다.
   *
   * @param scenarioTestService 시나리오 테스트 서비스
   */
  public ScenarioApiController(ScenarioTestService scenarioTestService) {
    this.scenarioTestService = scenarioTestService;
  }

  /**
   * 시나리오 테스트를 시작한다.
   *
   * @param request 테스트 요청
   * @return 테스트 ID
   */
  @PostMapping
  public ResponseEntity<Map<String, String>> startTest(@Valid @RequestBody ScenarioRequest request) {
    String testId = scenarioTestService.startTest(request);
    return ResponseEntity.ok(Map.of(
        "testId", testId,
        "status", "STARTED",
        "message", "Scenario test started"
    ));
  }

  /**
   * 테스트 결과를 조회한다.
   *
   * @param testId 테스트 ID
   * @return 테스트 결과
   */
  @GetMapping("/{testId}")
  public ResponseEntity<ScenarioResponse> getResult(@PathVariable String testId) {
    ScenarioResponse response = scenarioTestService.getResult(testId);
    if (response == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(response);
  }

  /**
   * 진행 중인 테스트 목록을 조회한다.
   *
   * @return 진행 중인 테스트 맵
   */
  @GetMapping("/running")
  public ResponseEntity<Map<String, String>> getRunningTests() {
    return ResponseEntity.ok(scenarioTestService.getRunningTests());
  }
}