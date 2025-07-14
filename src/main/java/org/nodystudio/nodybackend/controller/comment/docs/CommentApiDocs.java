package org.nodystudio.nodybackend.controller.comment.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.nodystudio.nodybackend.dto.comment.CommentCreateRequest;
import org.nodystudio.nodybackend.dto.comment.CommentResponse;
import org.nodystudio.nodybackend.dto.comment.CommentUpdateRequest;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Comment", description = "댓글 관리 API")
public interface CommentApiDocs {

  @Operation(
      summary = "댓글 생성",
      description = "스레드에 새로운 댓글을 작성합니다. 대댓글 작성 시 parentId를 지정하며, @username 형식으로 사용자를 멘션할 수 있습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "댓글 생성 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 입력값)"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "404", description = "스레드 또는 부모 댓글을 찾을 수 없음")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<CommentResponse>> createComment(
      @Parameter(description = "스레드 ID", required = true) @PathVariable Long threadId,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "댓글 생성 요청 정보", required = true) @Valid @RequestBody CommentCreateRequest request
  );

  @Operation(
      summary = "스레드의 댓글 목록 조회",
      description = "특정 스레드의 모든 댓글을 계층 구조로 조회합니다. 삭제된 댓글도 포함되어 대화 흐름을 유지합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "스레드를 찾을 수 없음")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<List<CommentResponse>>> getThreadComments(
      @Parameter(description = "스레드 ID", required = true) @PathVariable Long threadId
  );

  @Operation(
      summary = "댓글 수정",
      description = "작성한 댓글을 수정합니다. 삭제된 댓글은 수정할 수 없습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 수정 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (삭제된 댓글 수정 시도)"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없거나 수정 권한이 없음")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<CommentResponse>> updateComment(
      @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "댓글 수정 요청 정보", required = true) @Valid @RequestBody CommentUpdateRequest request
  );

  @Operation(
      summary = "댓글 삭제",
      description = "작성한 댓글을 삭제합니다. 소프트 삭제로 처리되어 대화 흐름은 유지됩니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 삭제된 댓글)"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없거나 삭제 권한이 없음")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<String>> deleteComment(
      @Parameter(description = "댓글 ID", required = true) @PathVariable Long commentId,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
  );

  @Operation(
      summary = "내가 작성한 댓글 목록 조회",
      description = "현재 사용자가 작성한 모든 댓글을 최신순으로 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "댓글 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))
      ),
      @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<CommentResponse>>> getMyComments(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "페이징 정보") Pageable pageable
  );

  @Operation(
      summary = "내가 멘션된 댓글 목록 조회",
      description = "현재 사용자가 @멘션된 모든 댓글을 최신순으로 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "멘션된 댓글 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = CommentResponse.class))
      ),
      @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  ResponseEntity<org.nodystudio.nodybackend.dto.ApiResponse<Page<CommentResponse>>> getMentionedComments(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "페이징 정보") Pageable pageable
  );
}