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
 * Load test REST API controller.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@RestController
@RequestMapping("${overload.dashboard.path:/overload}/api")
public class OverloadApiController {

  private final LoadTestService loadTestService;

  public OverloadApiController(LoadTestService loadTestService) {
    this.loadTestService = loadTestService;
  }

  @PostMapping("/tests")
  public ResponseEntity<TestResponse> startTest(@Valid @RequestBody TestRequest request) {
    String testId = UUID.randomUUID().toString().substring(0, 8);
    loadTestService.startTest(testId, request);
    return ResponseEntity.ok(new TestResponse(testId, "RUNNING"));
  }

  @GetMapping("/tests/{testId}")
  public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable("testId") String testId) {
    return ResponseEntity.ok(loadTestService.getTestStatus(testId));
  }

  @DeleteMapping("/tests/{testId}")
  public ResponseEntity<Void> stopTest(@PathVariable("testId") String testId) {
    loadTestService.stopTest(testId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/tests")
  public ResponseEntity<List<Map<String, Object>>> getRecentTests(
      @RequestParam(value = "limit", defaultValue = "10") int limit) {
    return ResponseEntity.ok(loadTestService.getRecentTests(limit));
  }
}