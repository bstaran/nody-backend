package org.nodystudio.nodybackend.controller.like.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.dto.like.LikeRequest;
import org.nodystudio.nodybackend.dto.like.LikeStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 좋아요 API 문서화 인터페이스
 */
@Tag(name = "좋아요 API", description = "스레드와 로그의 좋아요 관련 API")
public interface LikeApiDocs {

  @Operation(summary = "좋아요 토글", description = "좋아요를 추가하거나 취소합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<LikeStatusResponse>> toggleLike(
      @Valid @RequestBody LikeRequest request,
      Authentication authentication);

  @Operation(summary = "좋아요 상태 조회", description = "특정 대상의 좋아요 상태를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "좋아요 상태 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "404", description = "대상을 찾을 수 없음")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<LikeStatusResponse>> getLikeStatus(
      @Parameter(description = "대상 타입 (THREAD, LOG)", required = true) @RequestParam TargetType targetType,
      @Parameter(description = "대상 ID", required = true) @RequestParam Long targetId,
      Authentication authentication);
}