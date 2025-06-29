package org.nodystudio.nodybackend.controller.log;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.log.LogCreateRequest;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.log.LogSearchRequest;
import org.nodystudio.nodybackend.dto.log.LogUpdateRequest;
import org.nodystudio.nodybackend.service.log.LogService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * 로그 생성
     * POST /api/logs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LogResponse>> createLog(
            @Valid @RequestBody LogCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("로그 생성 요청 - 사용자: {}", userDetails.getUsername());
        
        LogResponse response = logService.createLog(request, userDetails.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("로그가 성공적으로 생성되었습니다.", response));
    }

    /**
     * 로그 단건 조회
     * GET /api/logs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LogResponse>> getLog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("로그 단건 조회 - ID: {}, 사용자: {}", id, 
                userDetails != null ? userDetails.getUsername() : "익명");
        
        LogResponse response = logService.getLog(id, 
                userDetails != null ? userDetails.getUsername() : null);
        
        return ResponseEntity.ok(ApiResponse.success("로그 조회가 완료되었습니다.", response));
    }

    /**
     * 로그 목록 조회 (위치 기반, 페이징)
     * GET /api/logs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LogResponse>>> getLogs(
            @Valid @ModelAttribute LogSearchRequest searchRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("로그 목록 조회 - 위치: ({}, {}), 반경: {}km, 사용자: {}", 
                searchRequest.getLatitude(), searchRequest.getLongitude(), 
                searchRequest.getRadiusKm(), 
                userDetails != null ? userDetails.getUsername() : "익명");
        
        Page<LogResponse> response = logService.searchLogs(searchRequest, 
                userDetails != null ? userDetails.getUsername() : null);
        
        return ResponseEntity.ok(ApiResponse.success("로그 목록 조회가 완료되었습니다.", response));
    }

    /**
     * 로그 수정
     * PUT /api/logs/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LogResponse>> updateLog(
            @PathVariable Long id,
            @Valid @RequestBody LogUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("로그 수정 요청 - ID: {}, 사용자: {}", id, userDetails.getUsername());
        
        LogResponse response = logService.updateLog(id, request, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success("로그가 성공적으로 수정되었습니다.", response));
    }

    /**
     * 로그 삭제
     * DELETE /api/logs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("로그 삭제 요청 - ID: {}, 사용자: {}", id, userDetails.getUsername());
        
        logService.deleteLog(id, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success("로그가 성공적으로 삭제되었습니다.", null));
    }
}