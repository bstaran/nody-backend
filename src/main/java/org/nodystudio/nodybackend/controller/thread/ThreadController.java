package org.nodystudio.nodybackend.controller.thread;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadSearchRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadUpdateRequest;
import org.nodystudio.nodybackend.service.thread.ThreadService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {

  private final ThreadService threadService;

  // === 공통 유틸리티 메소드 ===

  /**
   * 로그에 표시할 사용자 정보를 안전하게 가져옵니다. 인증되지 않은 사용자의 경우 "익명"을 반환합니다.
   */
  private String getUserDisplayName(User user) {
    return user != null ? user.getEmail() : "익명";
  }

  /**
   * 스레드 생성 POST /api/threads
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ThreadResponse>> createThread(
      @Valid @RequestBody ThreadCreateRequest request,
      @AuthenticationPrincipal User user) {

    log.info("스레드 생성 요청 - 사용자: {}",
        getUserDisplayName(user));

    ThreadResponse response = threadService.createThread(request, user.getEmail());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("스레드가 성공적으로 생성되었습니다.", response));
  }

  /**
   * 스레드 단건 조회 GET /api/threads/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ThreadResponse>> getThread(
      @PathVariable Long id,
      @AuthenticationPrincipal User user) {

    log.info("스레드 단건 조회 - ID: {}, 사용자: {}",
        id, getUserDisplayName(user));

    ThreadResponse response = threadService.getThread(
        id, user != null ? user.getEmail() : null);

    return ResponseEntity.ok(ApiResponse.success("스레드 조회가 완료되었습니다.", response));
  }

  /**
   * 스레드 목록 조회 (검색, 필터링, 페이징) GET /api/threads
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ThreadResponse>>> getThreads(
      @Valid @ModelAttribute ThreadSearchRequest searchRequest,
      @AuthenticationPrincipal User user) {

    log.info("스레드 목록 조회 - 키워드: {}, 타입: {}, 로그ID: {}, 사용자: {}",
        searchRequest.getKeyword(),
        searchRequest.getThreadType(),
        searchRequest.getLogId(),
        getUserDisplayName(user));

    Page<ThreadResponse> response = threadService.searchThreads(searchRequest,
        user != null ? user.getEmail() : null);

    return ResponseEntity.ok(ApiResponse.success("스레드 목록 조회가 완료되었습니다.", response));
  }

  /**
   * 스레드 수정 PUT /api/threads/{id}
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ThreadResponse>> updateThread(
      @PathVariable Long id,
      @Valid @RequestBody ThreadUpdateRequest request,
      @AuthenticationPrincipal User user) {

    log.info("스레드 수정 요청 - ID: {}, 사용자: {}", id, getUserDisplayName(user));

    ThreadResponse response = threadService.updateThread(id, request, user.getEmail());

    return ResponseEntity.ok(ApiResponse.success("스레드가 성공적으로 수정되었습니다.", response));
  }

  /**
   * 스레드 삭제 DELETE /api/threads/{id}
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteThread(
      @PathVariable Long id,
      @AuthenticationPrincipal User user) {

    log.info("스레드 삭제 요청 - ID: {}, 사용자: {}", id, getUserDisplayName(user));

    threadService.deleteThread(id, user.getEmail());

    return ResponseEntity.ok(ApiResponse.success("스레드가 성공적으로 삭제되었습니다.", null));
  }


  /**
   * 독립 스레드 목록 조회 (로그에 연결되지 않은 스레드) GET /api/threads/independent
   */
  @GetMapping("/independent")
  public ResponseEntity<ApiResponse<Page<ThreadResponse>>> getIndependentThreads(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDirection,
      @RequestParam(required = false) String keyword,
      @AuthenticationPrincipal User user) {

    log.info("독립 스레드 목록 조회 - 키워드: {}, 사용자: {}", keyword,
        getUserDisplayName(user));

    ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
        .page(page)
        .size(size)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .keyword(keyword)
        .threadType("independent")
        .build();

    Page<ThreadResponse> response = threadService.searchThreads(searchRequest,
        user != null ? user.getEmail() : null);

    return ResponseEntity.ok(ApiResponse.success("독립 스레드 목록 조회가 완료되었습니다.", response));
  }

  /**
   * 로그 연결 스레드 목록 조회 (로그에 연결된 스레드) GET /api/threads/linked
   */
  @GetMapping("/linked")
  public ResponseEntity<ApiResponse<Page<ThreadResponse>>> getLinkedThreads(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDirection,
      @RequestParam(required = false) String keyword,
      @AuthenticationPrincipal User user) {

    log.info("로그 연결 스레드 목록 조회 - 키워드: {}, 사용자: {}", keyword,
        getUserDisplayName(user));

    ThreadSearchRequest searchRequest = ThreadSearchRequest.builder()
        .page(page)
        .size(size)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .keyword(keyword)
        .threadType("linked")
        .build();

    Page<ThreadResponse> response = threadService.searchThreads(searchRequest,
        user != null ? user.getEmail() : null);

    return ResponseEntity.ok(ApiResponse.success("로그 연결 스레드 목록 조회가 완료되었습니다.", response));
  }
}