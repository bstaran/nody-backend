package org.nodystudio.nodybackend.controller.log;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.controller.log.docs.LogApiDocs;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.log.LogCreateRequest;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.log.LogSearchRequest;
import org.nodystudio.nodybackend.dto.log.LogUpdateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.nodystudio.nodybackend.service.log.LogService;
import org.nodystudio.nodybackend.service.thread.ThreadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController implements LogApiDocs {

  private final LogService logService;
  private final ThreadService threadService;

  /**
   * 로그에 표시할 사용자 정보를 안전하게 가져옵니다. 인증되지 않은 사용자의 경우 "익명"을 반환합니다.
   */
  private String getUserDisplayName(CustomUserDetails userDetails) {
    return userDetails != null ? userDetails.getEmail() : "익명";
  }

  /**
   * 로그 생성 POST /api/logs
   */
  @Override
  @PostMapping
  public ResponseEntity<ApiResponse<LogResponse>> createLog(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody LogCreateRequest request) {

    log.info("로그 생성 요청 - 사용자: {}", userDetails.getUser().getId());

    LogResponse response = logService.createLog(request, userDetails.getEmail());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("로그가 성공적으로 생성되었습니다.", response));
  }

  /**
   * 로그 단건 조회 GET /api/logs/{id}
   */
  @Override
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<LogResponse>> getLog(
      @PathVariable Long id,
      @Nullable @AuthenticationPrincipal CustomUserDetails userDetails) {

    log.info("로그 단건 조회 - ID: {}, 사용자: {}", id,
        getUserDisplayName(userDetails));

    LogResponse response = logService.getLog(id,
        userDetails != null ? userDetails.getEmail() : null);

    return ResponseEntity.ok(ApiResponse.success("로그 조회가 완료되었습니다.", response));
  }

  /**
   * 로그 목록 조회 (위치 기반, 페이징) GET /api/logs
   */
  @Override
  @GetMapping
  public ResponseEntity<ApiResponse<Page<LogResponse>>> getLogs(
      @Valid @ModelAttribute LogSearchRequest searchRequest,
      @PageableDefault(size = 20) Pageable pageable,
      @Nullable @AuthenticationPrincipal CustomUserDetails userDetails) {

    log.info("로그 목록 조회 - 위치: ({}, {}), 반경: {}km, 사용자: {}",
        searchRequest.getLatitude(), searchRequest.getLongitude(),
        searchRequest.getRadiusKm(),
        getUserDisplayName(userDetails));

    Page<LogResponse> response = logService.searchLogs(searchRequest,
        userDetails != null ? userDetails.getEmail() : null);

    return ResponseEntity.ok(ApiResponse.success("로그 목록 조회가 완료되었습니다.", response));
  }

  /**
   * 로그 수정 PUT /api/logs/{id}
   */
  @Override
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<LogResponse>> updateLog(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody LogUpdateRequest request) {

    log.info("로그 수정 요청 - ID: {}, 사용자: {}", id, userDetails.getUser().getId());

    LogResponse response = logService.updateLog(id, request, userDetails.getEmail());

    return ResponseEntity.ok(ApiResponse.success("로그가 성공적으로 수정되었습니다.", response));
  }

  /**
   * 로그 삭제 DELETE /api/logs/{id}
   */
  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteLog(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    log.info("로그 삭제 요청 - ID: {}, 사용자: {}", id, userDetails.getUser().getId());

    logService.deleteLog(id, userDetails.getEmail());

    return ResponseEntity.ok(ApiResponse.success("로그가 성공적으로 삭제되었습니다.", null));
  }

  /**
   * 특정 로그의 스레드 목록 조회 GET /api/logs/{logId}/threads
   */
  @Override
  @GetMapping("/{logId}/threads")
  public ResponseEntity<ApiResponse<Page<ThreadResponse>>> getLogThreads(
      @PathVariable Long logId,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
      @Nullable @AuthenticationPrincipal CustomUserDetails userDetails) {

    log.info("로그 스레드 목록 조회 - 로그ID: {}, 사용자: {}",
        logId, getUserDisplayName(userDetails));

    Page<ThreadResponse> response = threadService.getThreadsByLog(logId,
        userDetails != null ? userDetails.getEmail() : null, pageable);

    return ResponseEntity.ok(ApiResponse.success("로그 스레드 목록 조회가 완료되었습니다.", response));
  }
}