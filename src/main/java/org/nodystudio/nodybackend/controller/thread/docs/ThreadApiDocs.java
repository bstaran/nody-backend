package org.nodystudio.nodybackend.controller.thread.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.nodystudio.nodybackend.dto.thread.ThreadCreateRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadResponse;
import org.nodystudio.nodybackend.dto.thread.ThreadSearchRequest;
import org.nodystudio.nodybackend.dto.thread.ThreadUpdateRequest;
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
 * 스레드(댓글/답글) 관리 API 문서화 인터페이스
 */
@Tag(name = "스레드", description = "스레드(댓글/답글) CRUD 및 검색 API")
@SecurityRequirement(name = "bearerAuth")
public interface ThreadApiDocs {

  @Operation(
      summary = "스레드 생성",
      description = "새로운 스레드를 생성합니다. 로그에 연결된 스레드나 독립적인 스레드를 생성할 수 있습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "스레드 생성 성공",
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
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<ThreadResponse>> createThread(
      @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody ThreadCreateRequest request
  );

  @Operation(
      summary = "스레드 단건 조회",
      description = "ID로 특정 스레드를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "스레드 조회 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "404",
          description = "스레드를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<ThreadResponse>> getThread(
      @Parameter(description = "스레드 ID") @PathVariable Long id
  );

  @Operation(
      summary = "스레드 목록 조회",
      description = "스레드 목록을 검색 조건에 따라 조회합니다. 검색어, 로그 ID, 부모 스레드 ID 등으로 필터링할 수 있습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "스레드 목록 조회 성공",
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
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<ThreadResponse>>> getThreads(
      @Valid @ModelAttribute ThreadSearchRequest searchRequest,
      @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
  );

  @Operation(
      summary = "스레드 수정",
      description = "자신이 작성한 스레드를 수정합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "스레드 수정 성공",
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
          description = "권한 없음 (본인 스레드만 수정 가능)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "스레드를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<ThreadResponse>> updateThread(
      @Parameter(description = "스레드 ID") @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody ThreadUpdateRequest request
  );

  @Operation(
      summary = "스레드 삭제",
      description = "자신이 작성한 스레드를 삭제합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "스레드 삭제 성공",
          useReturnTypeSchema = true
      ),
      @ApiResponse(
          responseCode = "403",
          description = "권한 없음 (본인 스레드만 삭제 가능)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "스레드를 찾을 수 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = org.nodystudio.nodybackend.dto.ApiResponse.class)
          )
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Void>> deleteThread(
      @Parameter(description = "스레드 ID") @PathVariable Long id,
      @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails
  );

  @Operation(
      summary = "독립 스레드 목록 조회",
      description = "로그에 연결되지 않은 독립 스레드 목록을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "독립 스레드 목록 조회 성공",
          useReturnTypeSchema = true
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<ThreadResponse>>> getIndependentThreads(
      @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
  );

  @Operation(
      summary = "로그 연결 스레드 목록 조회",
      description = "로그에 연결된 스레드 목록을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그 연결 스레드 목록 조회 성공",
          useReturnTypeSchema = true
      )
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<ThreadResponse>>> getLinkedThreads(
      @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
  );
}