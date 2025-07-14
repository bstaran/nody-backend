package org.nodystudio.nodybackend.controller.comment;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.controller.comment.docs.CommentApiDocs;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.comment.CommentCreateRequest;
import org.nodystudio.nodybackend.dto.comment.CommentResponse;
import org.nodystudio.nodybackend.dto.comment.CommentUpdateRequest;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.nodystudio.nodybackend.service.comment.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController implements CommentApiDocs {

  private final CommentService commentService;

  /**
   * 댓글 생성 POST /api/threads/{threadId}/comments
   */
  @Override
  @PostMapping("/threads/{threadId}/comments")
  public ResponseEntity<ApiResponse<CommentResponse>> createComment(
      @PathVariable Long threadId,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody CommentCreateRequest request) {

    log.info("댓글 생성 요청 - 스레드: {}, 사용자: {}", threadId, userDetails.getEmail());

    CommentResponse response = commentService.createComment(threadId, request,
        userDetails.getEmail());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("댓글이 성공적으로 작성되었습니다.", response));
  }

  /**
   * 스레드의 댓글 목록 조회 GET /api/threads/{threadId}/comments
   */
  @Override
  @GetMapping("/threads/{threadId}/comments")
  public ResponseEntity<ApiResponse<List<CommentResponse>>> getThreadComments(
      @PathVariable Long threadId) {

    log.info("스레드 댓글 목록 조회 - 스레드: {}", threadId);

    List<CommentResponse> response = commentService.getThreadComments(threadId);

    return ResponseEntity.ok(
        ApiResponse.success(String.format("%d개의 댓글을 조회했습니다.", response.size()), response));
  }

  /**
   * 댓글 수정 PUT /api/comments/{commentId}
   */
  @Override
  @PutMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
      @PathVariable Long commentId,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody CommentUpdateRequest request) {

    log.info("댓글 수정 요청 - ID: {}, 사용자: {}", commentId, userDetails.getEmail());

    CommentResponse response = commentService.updateComment(commentId, request,
        userDetails.getEmail());

    return ResponseEntity.ok(ApiResponse.success("댓글이 성공적으로 수정되었습니다.", response));
  }

  /**
   * 댓글 삭제 DELETE /api/comments/{commentId}
   */
  @Override
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<String>> deleteComment(
      @PathVariable Long commentId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {

    log.info("댓글 삭제 요청 - ID: {}, 사용자: {}", commentId, userDetails.getEmail());

    commentService.deleteComment(commentId, userDetails.getEmail());

    return ResponseEntity.ok(ApiResponse.success("댓글이 성공적으로 삭제되었습니다.", null));
  }

  /**
   * 내가 작성한 댓글 목록 조회 GET /api/user/comments
   */
  @Override
  @GetMapping("/user/comments")
  public ResponseEntity<ApiResponse<Page<CommentResponse>>> getMyComments(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

    log.info("내 댓글 목록 조회 - 사용자: {}", userDetails.getEmail());

    Page<CommentResponse> response = commentService.getUserComments(userDetails.getEmail(),
        pageable);

    return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회가 완료되었습니다.", response));
  }

  /**
   * 내가 멘션된 댓글 목록 조회 GET /api/user/mentions
   */
  @Override
  @GetMapping("/user/mentions")
  public ResponseEntity<ApiResponse<Page<CommentResponse>>> getMentionedComments(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

    log.info("멘션된 댓글 목록 조회 - 사용자: {}", userDetails.getEmail());

    Page<CommentResponse> response = commentService.getMentionedComments(userDetails.getEmail(),
        pageable);

    return ResponseEntity.ok(ApiResponse.success("멘션된 댓글 목록 조회가 완료되었습니다.", response));
  }

  /**
   * 스레드의 댓글 개수 조회 GET /api/threads/{threadId}/comments/count
   */
  @GetMapping("/threads/{threadId}/comments/count")
  public ResponseEntity<ApiResponse<Long>> getCommentCount(@PathVariable Long threadId) {

    log.info("스레드 댓글 개수 조회 - 스레드: {}", threadId);

    long count = commentService.getCommentCount(threadId);

    return ResponseEntity.ok(ApiResponse.success("댓글 개수 조회가 완료되었습니다.", count));
  }
}