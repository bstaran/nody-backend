package org.nodystudio.nodybackend.controller.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.comment.CommentCreateRequest;
import org.nodystudio.nodybackend.dto.comment.CommentResponse;
import org.nodystudio.nodybackend.dto.comment.CommentUpdateRequest;
import org.nodystudio.nodybackend.dto.user.UserSummaryResponse;
import org.nodystudio.nodybackend.exception.custom.BadRequestException;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.security.userdetails.CustomUserDetails;
import org.nodystudio.nodybackend.service.comment.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController 테스트")
class CommentControllerTest {

  private static final String MOCK_USER_EMAIL = "testuser@example.com";
  @Mock
  private CommentService commentService;
  @InjectMocks
  private CommentController commentController;

  /**
   * 테스트용 CustomUserDetails 객체를 생성합니다.
   */
  private CustomUserDetails createMockUserDetails() {
    User mockUser = mock(User.class);
    lenient().when(mockUser.getId()).thenReturn(1L);
    lenient().when(mockUser.getEmail()).thenReturn(MOCK_USER_EMAIL);
    lenient().when(mockUser.getRoles())
        .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));
    lenient().when(mockUser.getIsActive()).thenReturn(true);

    return new CustomUserDetails(mockUser);
  }

  @Nested
  @DisplayName("댓글 생성 (POST /api/threads/{threadId}/comments)")
  class CreateComment {

    @Test
    @DisplayName("성공: 유효한 요청으로 댓글을 성공적으로 생성한다")
    void createComment_Success() {
      // given
      long threadId = 1L;
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("새로운 댓글입니다.")
          .build();

      UserSummaryResponse author = UserSummaryResponse.builder()
          .id(1L)
          .nickname("testuser")
          .build();

      CommentResponse response = CommentResponse.builder()
          .id(1L)
          .content("새로운 댓글입니다.")
          .author(author)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      given(commentService.createComment(eq(threadId), any(CommentCreateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willReturn(response);

      CustomUserDetails userDetails = createMockUserDetails();

      // when
      ResponseEntity<ApiResponse<CommentResponse>> result = commentController.createComment(
          threadId, userDetails,
          request);

      // then
      assertEquals(HttpStatus.CREATED, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<CommentResponse> body = Objects.requireNonNull(result.getBody());
      assertEquals(1L, body.getData().getId());
      assertEquals("새로운 댓글입니다.", body.getData().getContent());
      assertEquals("댓글이 성공적으로 작성되었습니다.", body.getMessage());

      verify(commentService).createComment(eq(threadId), any(CommentCreateRequest.class),
          eq(MOCK_USER_EMAIL));
    }

    @Test
    @DisplayName("성공: 대댓글을 성공적으로 생성한다")
    void createReply_Success() {
      // given
      long threadId = 1L;
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글입니다.")
          .parentId(1L)
          .build();

      UserSummaryResponse author = UserSummaryResponse.builder()
          .id(1L)
          .nickname("testuser")
          .build();

      CommentResponse response = CommentResponse.builder()
          .id(2L)
          .content("대댓글입니다.")
          .author(author)
          .parentId(1L)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      given(commentService.createComment(eq(threadId), any(CommentCreateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willReturn(response);

      CustomUserDetails userDetails = createMockUserDetails();

      // when
      ResponseEntity<ApiResponse<CommentResponse>> result = commentController.createComment(
          threadId, userDetails,
          request);

      // then
      assertEquals(HttpStatus.CREATED, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<CommentResponse> body = Objects.requireNonNull(result.getBody());
      assertEquals(1L, body.getData().getParentId());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 스레드는 ResourceNotFoundException을 던진다")
    void createComment_Failure_ThreadNotFound() {
      // given
      long threadId = 999L;
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("댓글")
          .build();

      given(commentService.createComment(eq(threadId), any(CommentCreateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다."));

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.createComment(threadId, userDetails, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("스레드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 부모 댓글은 ResourceNotFoundException을 던진다")
    void createComment_Failure_ParentNotFound() {
      // given
      long threadId = 1L;
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글")
          .parentId(999L)
          .build();

      given(commentService.createComment(eq(threadId), any(CommentCreateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willThrow(new ResourceNotFoundException("부모 댓글을 찾을 수 없습니다."));

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.createComment(threadId, userDetails, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("부모 댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 다른 스레드의 부모 댓글은 BadRequestException을 던진다")
    void createComment_Failure_ParentFromDifferentThread() {
      // given
      long threadId = 1L;
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글")
          .parentId(2L)
          .build();

      given(commentService.createComment(eq(threadId), any(CommentCreateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willThrow(new BadRequestException("부모 댓글이 다른 스레드에 속해 있습니다."));

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.createComment(threadId, userDetails, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("부모 댓글이 다른 스레드에 속해 있습니다");
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회 (GET /api/threads/{threadId}/comments)")
  class GetThreadComments {

    @Test
    @DisplayName("성공: 스레드의 댓글 목록을 계층 구조로 조회한다")
    void getThreadComments_Success() {
      // given
      long threadId = 1L;

      UserSummaryResponse author = UserSummaryResponse.builder()
          .id(1L)
          .nickname("testuser")
          .build();

      CommentResponse childComment = CommentResponse.builder()
          .id(2L)
          .content("자식 댓글")
          .author(author)
          .parentId(1L)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      CommentResponse rootComment = CommentResponse.builder()
          .id(1L)
          .content("부모 댓글")
          .author(author)
          .children(List.of(childComment))
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      given(commentService.getThreadComments(threadId))
          .willReturn(List.of(rootComment));

      // when
      ResponseEntity<ApiResponse<List<CommentResponse>>> result = commentController.getThreadComments(
          threadId);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<List<CommentResponse>> body = Objects.requireNonNull(result.getBody());
      assertEquals(1, body.getData().size());
      assertEquals(1L, body.getData().get(0).getId());
      assertEquals(1, body.getData().get(0).getChildren().size());

      verify(commentService).getThreadComments(threadId);
    }

    @Test
    @DisplayName("성공: 댓글이 없는 스레드는 빈 목록을 반환한다")
    void getThreadComments_Success_Empty() {
      // given
      long threadId = 1L;

      given(commentService.getThreadComments(threadId))
          .willReturn(Collections.emptyList());

      // when
      ResponseEntity<ApiResponse<List<CommentResponse>>> result = commentController.getThreadComments(
          threadId);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<List<CommentResponse>> body = Objects.requireNonNull(result.getBody());
      assertThat(body.getData()).isEmpty();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 스레드는 ResourceNotFoundException을 던진다")
    void getThreadComments_Failure_ThreadNotFound() {
      // given
      long threadId = 999L;

      given(commentService.getThreadComments(threadId))
          .willThrow(new ResourceNotFoundException("스레드를 찾을 수 없습니다."));

      // when & then
      assertThatThrownBy(() -> commentController.getThreadComments(threadId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("스레드를 찾을 수 없습니다");
    }
  }

  @Nested
  @DisplayName("댓글 수정 (PUT /api/comments/{commentId})")
  class UpdateComment {

    @Test
    @DisplayName("성공: 자신의 댓글을 성공적으로 수정한다")
    void updateComment_Success() {
      // given
      long commentId = 1L;
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("수정된 댓글 내용")
          .build();

      UserSummaryResponse author = UserSummaryResponse.builder()
          .id(1L)
          .nickname("testuser")
          .build();

      CommentResponse response = CommentResponse.builder()
          .id(commentId)
          .content("수정된 댓글 내용")
          .author(author)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      given(commentService.updateComment(eq(commentId), any(CommentUpdateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willReturn(response);

      CustomUserDetails userDetails = createMockUserDetails();

      // when
      ResponseEntity<ApiResponse<CommentResponse>> result = commentController.updateComment(
          commentId, userDetails,
          request);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<CommentResponse> body = Objects.requireNonNull(result.getBody());
      assertEquals("수정된 댓글 내용", body.getData().getContent());
      assertEquals("댓글이 성공적으로 수정되었습니다.", body.getMessage());

      verify(commentService).updateComment(eq(commentId), any(CommentUpdateRequest.class),
          eq(MOCK_USER_EMAIL));
    }

    @Test
    @DisplayName("실패: 다른 사용자의 댓글 수정은 ResourceNotFoundException을 던진다")
    void updateComment_Failure_NotOwner() {
      // given
      long commentId = 1L;
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("수정 시도")
          .build();

      given(commentService.updateComment(eq(commentId), any(CommentUpdateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willThrow(new ResourceNotFoundException("수정 권한이 없거나 댓글을 찾을 수 없습니다."));

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.updateComment(commentId, userDetails, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("수정 권한이 없거나 댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 삭제된 댓글 수정은 BadRequestException을 던진다")
    void updateComment_Failure_DeletedComment() {
      // given
      long commentId = 1L;
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("삭제된 댓글 수정 시도")
          .build();

      given(commentService.updateComment(eq(commentId), any(CommentUpdateRequest.class),
          eq(MOCK_USER_EMAIL)))
          .willThrow(new BadRequestException("삭제된 댓글은 수정할 수 없습니다."));

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.updateComment(commentId, userDetails, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("삭제된 댓글은 수정할 수 없습니다");
    }
  }

  @Nested
  @DisplayName("댓글 삭제 (DELETE /api/comments/{commentId})")
  class DeleteComment {

    @Test
    @DisplayName("성공: 자신의 댓글을 성공적으로 삭제한다")
    void deleteComment_Success() {
      // given
      long commentId = 1L;

      willDoNothing().given(commentService).deleteComment(commentId, MOCK_USER_EMAIL);

      CustomUserDetails userDetails = createMockUserDetails();

      // when
      ResponseEntity<ApiResponse<String>> result = commentController.deleteComment(commentId,
          userDetails);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<String> body = Objects.requireNonNull(result.getBody());
      assertEquals("댓글이 성공적으로 삭제되었습니다.", body.getMessage());

      verify(commentService).deleteComment(commentId, MOCK_USER_EMAIL);
    }

    @Test
    @DisplayName("실패: 다른 사용자의 댓글 삭제는 ResourceNotFoundException을 던진다")
    void deleteComment_Failure_NotOwner() {
      // given
      long commentId = 1L;

      willThrow(new ResourceNotFoundException("수정 권한이 없거나 댓글을 찾을 수 없습니다."))
          .given(commentService).deleteComment(commentId, MOCK_USER_EMAIL);

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.deleteComment(commentId, userDetails))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("수정 권한이 없거나 댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 이미 삭제된 댓글 삭제는 BadRequestException을 던진다")
    void deleteComment_Failure_AlreadyDeleted() {
      // given
      long commentId = 1L;

      willThrow(new BadRequestException("이미 삭제된 댓글입니다."))
          .given(commentService).deleteComment(commentId, MOCK_USER_EMAIL);

      CustomUserDetails userDetails = createMockUserDetails();

      // when & then
      assertThatThrownBy(() -> commentController.deleteComment(commentId, userDetails))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("이미 삭제된 댓글입니다");
    }
  }

  @Nested
  @DisplayName("사용자 댓글 조회 (GET /api/comments/user)")
  class GetUserComments {

    @Test
    @DisplayName("성공: 사용자가 작성한 댓글 목록을 페이징으로 조회한다")
    void getUserComments_Success() {
      // given
      Pageable pageable = PageRequest.of(0, 20);

      UserSummaryResponse author = UserSummaryResponse.builder()
          .id(1L)
          .nickname("testuser")
          .build();

      CommentResponse comment = CommentResponse.builder()
          .id(1L)
          .content("내 댓글")
          .author(author)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      Page<CommentResponse> commentPage = new PageImpl<>(List.of(comment), pageable, 1);

      given(commentService.getUserComments(MOCK_USER_EMAIL, pageable))
          .willReturn(commentPage);

      CustomUserDetails userDetails = createMockUserDetails();

      // when
      ResponseEntity<ApiResponse<Page<CommentResponse>>> result = commentController.getMyComments(
          userDetails,
          pageable);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<Page<CommentResponse>> body = Objects.requireNonNull(result.getBody());
      assertEquals(1, body.getData().getContent().size());
      assertEquals("내 댓글", body.getData().getContent().get(0).getContent());

      verify(commentService).getUserComments(MOCK_USER_EMAIL, pageable);
    }
  }

  @Nested
  @DisplayName("멘션된 댓글 조회 (GET /api/comments/mentions)")
  class GetMentionedComments {

    @Test
    @DisplayName("성공: 사용자가 멘션된 댓글 목록을 페이징으로 조회한다")
    void getMentionedComments_Success() {
      // given
      Pageable pageable = PageRequest.of(0, 20);

      UserSummaryResponse author = UserSummaryResponse.builder()
          .id(2L)
          .nickname("other")
          .build();

      CommentResponse comment = CommentResponse.builder()
          .id(1L)
          .content("@testuser 안녕하세요!")
          .author(author)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .isDeleted(false)
          .build();

      Page<CommentResponse> commentPage = new PageImpl<>(List.of(comment), pageable, 1);

      given(commentService.getMentionedComments(MOCK_USER_EMAIL, pageable))
          .willReturn(commentPage);

      CustomUserDetails userDetails = createMockUserDetails();

      // when
      ResponseEntity<ApiResponse<Page<CommentResponse>>> result = commentController.getMentionedComments(
          userDetails,
          pageable);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<Page<CommentResponse>> body = Objects.requireNonNull(result.getBody());
      assertEquals(1, body.getData().getContent().size());
      assertEquals("@testuser 안녕하세요!", body.getData().getContent().get(0).getContent());

      verify(commentService).getMentionedComments(MOCK_USER_EMAIL, pageable);
    }
  }

  @Nested
  @DisplayName("댓글 개수 조회 (GET /api/threads/{threadId}/comments/count)")
  class GetCommentCount {

    @Test
    @DisplayName("성공: 스레드의 활성 댓글 개수를 조회한다")
    void getCommentCount_Success() {
      // given
      long threadId = 1L;
      long expectedCount = 5L;

      given(commentService.getCommentCount(threadId))
          .willReturn(expectedCount);

      // when
      ResponseEntity<ApiResponse<Long>> result = commentController.getCommentCount(threadId);

      // then
      assertEquals(HttpStatus.OK, result.getStatusCode());
      assertNotNull(result.getBody());
      ApiResponse<Long> body = Objects.requireNonNull(result.getBody());
      assertEquals(expectedCount, body.getData());
      assertEquals("댓글 개수 조회가 완료되었습니다.", body.getMessage());

      verify(commentService).getCommentCount(threadId);
    }
  }
}