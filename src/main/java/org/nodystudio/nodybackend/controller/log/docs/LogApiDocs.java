package org.nodystudio.nodybackend.controller.log.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.nodystudio.nodybackend.dto.log.LogCreateRequest;
import org.nodystudio.nodybackend.dto.log.LogResponse;
import org.nodystudio.nodybackend.dto.log.LogSearchRequest;
import org.nodystudio.nodybackend.dto.log.LogUpdateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 로그(게시물) 관리 API 문서화 인터페이스
 */
@Tag(name = "로그", description = "로그(게시물) CRUD 및 위치 기반 검색 API")
@SecurityRequirement(name = "bearerAuth")
public interface LogApiDocs {

  @Operation(
      summary = "로그 생성",
      description = "새로운 로그를 생성합니다. 위치 정보와 내용을 포함할 수 있습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "로그 생성 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "400",
          description = "유효하지 않은 요청 데이터",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증되지 않은 사용자",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<LogResponse>> createLog(
      @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody LogCreateRequest request
  );

  @Operation(
      summary = "로그 단건 조회",
      description = "ID로 특정 로그를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그 조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "404",
          description = "로그를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<LogResponse>> getLog(
      @Parameter(description = "로그 ID") @PathVariable Long id
  );

  @Operation(
      summary = "로그 목록 조회",
      description = "위치 기반으로 로그 목록을 조회합니다. 거리 범위 내의 로그들을 페이징하여 반환합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그 목록 조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 검색 조건",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<LogResponse>>> getLogs(
      @Valid @ModelAttribute LogSearchRequest searchRequest,
      @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable
  );

  @Operation(
      summary = "로그 수정",
      description = "자신이 작성한 로그를 수정합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그 수정 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "400",
          description = "유효하지 않은 요청 데이터",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음 (본인 로그만 수정 가능)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "로그를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<LogResponse>> updateLog(
      @Parameter(description = "로그 ID") @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody LogUpdateRequest request
  );

  @Operation(
      summary = "로그 삭제",
      description = "자신이 작성한 로그를 삭제합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그 삭제 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음 (본인 로그만 삭제 가능)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "로그를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Void>> deleteLog(
      @Parameter(description = "로그 ID") @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
  );

  @Operation(
      summary = "특정 로그의 스레드 목록 조회",
      description = "특정 로그에 연결된 스레드(댓글) 목록을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "스레드 목록 조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "404",
          description = "로그를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<ThreadResponse>>> getLogThreads(
      @Parameter(description = "로그 ID") @PathVariable Long logId,
      @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
  );
}