package org.nodystudio.nodybackend.controller.admin.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 관리자 배치 작업 API 문서화 인터페이스
 */
@Tag(name = "관리자 배치", description = "관리자용 배치 작업 수동 실행 및 모니터링 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public interface AdminBatchApiDocs {

  @Operation(
      summary = "만료된 탈퇴 사용자 수동 정리",
      description = "탈퇴 후 30일이 경과한 사용자 데이터를 수동으로 정리합니다. ADMIN 권한이 필요합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "배치 작업 실행 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음 (ADMIN 권한 필요)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Map<String, Object>>> runUserCleanupBatch();

  @Operation(
      summary = "삭제 예정 사용자 수 조회",
      description = "탈퇴 후 30일이 경과하여 삭제 예정인 사용자 수를 조회합니다. ADMIN 권한이 필요합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음 (ADMIN 권한 필요)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Long>> getExpiredUsersCount();

  @Operation(
      summary = "배치 작업 상태 정보 조회",
      description = "사용자 정리 배치 작업의 상태 정보를 조회합니다. ADMIN 권한이 필요합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "상태 정보 조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음 (ADMIN 권한 필요)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Map<String, Object>>> getBatchStatus();
}