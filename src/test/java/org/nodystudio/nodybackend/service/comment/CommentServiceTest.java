package org.nodystudio.nodybackend.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.comment.Comment;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.comment.CommentCreateRequest;
import org.nodystudio.nodybackend.dto.comment.CommentResponse;
import org.nodystudio.nodybackend.dto.comment.CommentUpdateRequest;
import org.nodystudio.nodybackend.exception.custom.BadRequestException;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.CommentRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 테스트")
class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private ThreadRepository threadRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private CommentService commentService;

  private User createMockUser(Long id, String email, String nickname) {
    return User.builder()
        .id(id)
        .provider(org.nodystudio.nodybackend.domain.enums.OAuthProvider.GOOGLE)
        .socialId("test-social-id-" + id)
        .email(email)
        .nickname(nickname)
        .isActive(true)
        .build();
  }

  private Thread createMockThread(Long id, String content) {
    return Thread.builder()
        .id(id)
        .content(content)
        .user(createMockUser(1L, "author@example.com", "author"))
        .build();
  }

  private Comment createMockComment(Long id, String content, User author, Thread thread) {
    return Comment.builder()
        .id(id)
        .content(content)
        .author(author)
        .thread(thread)
        .mentionedUsers(new HashSet<>())
        .build();
  }

  @Nested
  @DisplayName("댓글 생성 (createComment)")
  class CreateComment {

    @Test
    @DisplayName("성공: 유효한 요청으로 댓글을 성공적으로 생성한다")
    void createComment_Success() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("새로운 댓글입니다.")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");
      Comment mockComment = createMockComment(1L, request.getContent(), mockUser, mockThread);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      CommentResponse result = commentService.createComment(threadId, request, userEmail);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getContent()).isEqualTo("새로운 댓글입니다.");
      assertThat(result.getAuthor().getNickname()).isEqualTo("testuser");

      then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    @DisplayName("성공: 대댓글을 성공적으로 생성한다")
    void createReply_Success() {
      // given
      Long threadId = 1L;
      Long parentId = 2L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글입니다.")
          .parentId(parentId)
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");
      Comment parentComment = createMockComment(parentId, "부모 댓글", mockUser, mockThread);
      Comment replyComment = createMockComment(3L, request.getContent(), mockUser, mockThread);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(commentRepository.findById(parentId)).willReturn(Optional.of(parentComment));
      given(commentRepository.save(any(Comment.class))).willReturn(replyComment);

      // when
      CommentResponse result = commentService.createComment(threadId, request, userEmail);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("대댓글입니다.");

      ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
      then(commentRepository).should().save(commentCaptor.capture());

      Comment savedComment = commentCaptor.getValue();
      assertThat(savedComment.getParent()).isEqualTo(parentComment);
    }

    // TODO: Issue #80 - 멘션 기능 구현 시 활성화
    // @Test
    // @DisplayName("성공: 멘션이 포함된 댓글을 생성하고 이벤트를 발행한다")
    // void createCommentWithMention_Success() {
    //   // given
    //   Long threadId = 1L;
    //   String userEmail = "testuser@example.com";
    //   CommentCreateRequest request = CommentCreateRequest.builder()
    //       .content("@mentioneduser 안녕하세요!")
    //       .build();

    //   User mockUser = createMockUser(1L, userEmail, "testuser");
    //   User mentionedUser = createMockUser(2L, "mentioneduser@example.com", "mentioneduser");
    //   Thread mockThread = createMockThread(threadId, "테스트 스레드");

    //   Comment mockComment = Comment.builder()
    //       .id(1L)
    //       .content(request.getContent())
    //       .author(mockUser)
    //       .thread(mockThread)
    //       .mentionedUsers(Set.of(mentionedUser))
    //       .build();

    //   given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
    //   given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
    //   given(userRepository.findByEmail("mentioneduser@example.com")).willReturn(
    //       Optional.of(mentionedUser));
    //   given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

    //   // when
    //   commentService.createComment(threadId, request, userEmail);

    //   // then
    //   ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(
    //       CommentMentionEvent.class);
    //   then(eventPublisher).should().publishEvent(eventCaptor.capture());

    //   CommentMentionEvent event = eventCaptor.getValue();
    //   assertThat(event.getCommentId()).isEqualTo(1L);
    //   assertThat(event.getAuthorId()).isEqualTo(1L);
    //   assertThat(event.getThreadId()).isEqualTo(threadId);
    //   assertThat(event.getMentionedUserIds()).contains(2L);
    // }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자는 UserNotFoundException을 던진다")
    void createComment_Failure_UserNotFound() {
      // given
      Long threadId = 1L;
      String userEmail = "nonexistent@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("댓글")
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.createComment(threadId, request, userEmail))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 스레드는 ResourceNotFoundException을 던진다")
    void createComment_Failure_ThreadNotFound() {
      // given
      Long threadId = 999L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("댓글")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.createComment(threadId, request, userEmail))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("스레드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 부모 댓글은 ResourceNotFoundException을 던진다")
    void createComment_Failure_ParentNotFound() {
      // given
      Long threadId = 1L;
      Long parentId = 999L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글")
          .parentId(parentId)
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(commentRepository.findById(parentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.createComment(threadId, request, userEmail))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("부모 댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 다른 스레드의 부모 댓글은 BadRequestException을 던진다")
    void createComment_Failure_ParentFromDifferentThread() {
      // given
      Long threadId = 1L;
      Long parentId = 2L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글")
          .parentId(parentId)
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");
      Thread otherThread = createMockThread(3L, "다른 스레드");
      Comment parentComment = createMockComment(parentId, "부모 댓글", mockUser, otherThread);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(commentRepository.findById(parentId)).willReturn(Optional.of(parentComment));

      // when & then
      assertThatThrownBy(() -> commentService.createComment(threadId, request, userEmail))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("부모 댓글이 다른 스레드에 속해 있습니다");
    }

    @Test
    @DisplayName("실패: 삭제된 부모 댓글에 대댓글 작성은 BadRequestException을 던진다")
    void createComment_Failure_DeletedParent() {
      // given
      Long threadId = 1L;
      Long parentId = 2L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("대댓글")
          .parentId(parentId)
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");
      Comment parentComment = Comment.builder()
          .id(parentId)
          .content("삭제된 댓글")
          .author(mockUser)
          .thread(mockThread)
          .deletedAt(LocalDateTime.now()) // 삭제된 댓글
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(commentRepository.findById(parentId)).willReturn(Optional.of(parentComment));

      // when & then
      assertThatThrownBy(() -> commentService.createComment(threadId, request, userEmail))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("삭제된 댓글에는 답글을 작성할 수 없습니다");
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회 (getThreadComments)")
  class GetThreadComments {

    @Test
    @DisplayName("성공: 스레드의 댓글 목록을 계층 구조로 조회한다")
    void getThreadComments_Success() {
      // given
      Long threadId = 1L;
      User mockUser = createMockUser(1L, "testuser@example.com", "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment rootComment = Comment.builder()
          .id(1L)
          .content("부모 댓글")
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(new HashSet<>())
          .build();

      Comment childComment = Comment.builder()
          .id(2L)
          .content("자식 댓글")
          .author(mockUser)
          .thread(mockThread)
          .parent(rootComment)
          .mentionedUsers(new HashSet<>())
          .build();

      given(threadRepository.existsById(threadId)).willReturn(true);
      given(commentRepository.findByThreadIdWithAuthorAndMentions(threadId))
          .willReturn(Arrays.asList(rootComment, childComment));

      // when
      List<CommentResponse> result = commentService.getThreadComments(threadId);

      // then
      assertThat(result).hasSize(1); // 루트 댓글만 반환

      CommentResponse rootResponse = result.get(0);
      assertThat(rootResponse.getId()).isEqualTo(1L);
      assertThat(rootResponse.getContent()).isEqualTo("부모 댓글");
      assertThat(rootResponse.getChildren()).hasSize(1);

      CommentResponse childResponse = rootResponse.getChildren().get(0);
      assertThat(childResponse.getId()).isEqualTo(2L);
      assertThat(childResponse.getContent()).isEqualTo("자식 댓글");
    }

    @Test
    @DisplayName("성공: 댓글이 없는 스레드는 빈 목록을 반환한다")
    void getThreadComments_Success_Empty() {
      // given
      Long threadId = 1L;

      given(threadRepository.existsById(threadId)).willReturn(true);
      given(commentRepository.findByThreadIdWithAuthorAndMentions(threadId))
          .willReturn(Collections.emptyList());

      // when
      List<CommentResponse> result = commentService.getThreadComments(threadId);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 스레드는 ResourceNotFoundException을 던진다")
    void getThreadComments_Failure_ThreadNotFound() {
      // given
      Long threadId = 999L;

      given(threadRepository.existsById(threadId)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> commentService.getThreadComments(threadId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("스레드를 찾을 수 없습니다");
    }
  }

  @Nested
  @DisplayName("댓글 수정 (updateComment)")
  class UpdateComment {

    @Test
    @DisplayName("성공: 자신의 댓글을 성공적으로 수정한다")
    void updateComment_Success() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("수정된 댓글 내용")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(1L, "테스트 스레드");
      Comment mockComment = Comment.builder()
          .id(commentId)
          .content("원본 댓글 내용")
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(new HashSet<>())
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(mockComment));

      // when
      CommentResponse result = commentService.updateComment(commentId, request, userEmail);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("수정된 댓글 내용");
    }

    // TODO: Issue #80 - 멘션 기능 구현 시 활성화
    // @Test
    // @DisplayName("성공: 멘션이 추가된 댓글 수정 시 새로운 멘션에 대한 이벤트만 발행한다")
    // void updateComment_Success_WithNewMentions() {
    //   // given
    //   Long commentId = 1L;
    //   String userEmail = "testuser@example.com";
    //   CommentUpdateRequest request = CommentUpdateRequest.builder()
    //       .content("@newuser 추가 멘션과 @olduser 기존 멘션")
    //       .build();

    //   User mockUser = createMockUser(1L, userEmail, "testuser");
    //   User oldUser = createMockUser(2L, "olduser@example.com", "olduser");
    //   User newUser = createMockUser(3L, "newuser@example.com", "newuser");
    //   Thread mockThread = createMockThread(1L, "테스트 스레드");

    //   Comment mockComment = Comment.builder()
    //       .id(commentId)
    //       .content("@olduser 기존 멘션")
    //       .author(mockUser)
    //       .thread(mockThread)
    //       .mentionedUsers(new HashSet<>(Set.of(oldUser)))
    //       .build();

    //   given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
    //   given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
    //       .willReturn(Optional.of(mockComment));
    //   given(userRepository.findByEmail("olduser@example.com")).willReturn(Optional.of(oldUser));
    //   given(userRepository.findByEmail("newuser@example.com")).willReturn(Optional.of(newUser));

    //   // when
    //   commentService.updateComment(commentId, request, userEmail);

    //   // then
    //   ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(
    //       CommentMentionEvent.class);
    //   then(eventPublisher).should().publishEvent(eventCaptor.capture());

    //   CommentMentionEvent event = eventCaptor.getValue();
    //   assertThat(event.getMentionedUserIds()).contains(3L); // 새로운 멘션만
    //   assertThat(event.getMentionedUserIds()).doesNotContain(2L); // 기존 멘션 제외
    // }

    @Test
    @DisplayName("실패: 다른 사용자의 댓글 수정은 ResourceNotFoundException을 던진다")
    void updateComment_Failure_NotOwner() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("수정 시도")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.updateComment(commentId, request, userEmail))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("수정 권한이 없거나 댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 삭제된 댓글 수정은 BadRequestException을 던진다")
    void updateComment_Failure_DeletedComment() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("삭제된 댓글 수정 시도")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(1L, "테스트 스레드");
      Comment deletedComment = Comment.builder()
          .id(commentId)
          .content("삭제된 댓글")
          .author(mockUser)
          .thread(mockThread)
          .deletedAt(LocalDateTime.now())
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(deletedComment));

      // when & then
      assertThatThrownBy(() -> commentService.updateComment(commentId, request, userEmail))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("삭제된 댓글은 수정할 수 없습니다");
    }
  }

  @Nested
  @DisplayName("댓글 삭제 (deleteComment)")
  class DeleteComment {

    @Test
    @DisplayName("성공: 자신의 댓글을 성공적으로 삭제한다")
    void deleteComment_Success() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(1L, "테스트 스레드");
      Comment mockComment = createMockComment(commentId, "삭제할 댓글", mockUser, mockThread);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(mockComment));
      willDoNothing().given(commentRepository).delete(mockComment);

      // when
      commentService.deleteComment(commentId, userEmail);

      // then
      then(commentRepository).should().delete(mockComment);
    }

    @Test
    @DisplayName("실패: 다른 사용자의 댓글 삭제는 ResourceNotFoundException을 던진다")
    void deleteComment_Failure_NotOwner() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";

      User mockUser = createMockUser(1L, userEmail, "testuser");

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> commentService.deleteComment(commentId, userEmail))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("수정 권한이 없거나 댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("실패: 이미 삭제된 댓글 삭제는 BadRequestException을 던진다")
    void deleteComment_Failure_AlreadyDeleted() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(1L, "테스트 스레드");
      Comment deletedComment = Comment.builder()
          .id(commentId)
          .content("이미 삭제된 댓글")
          .author(mockUser)
          .thread(mockThread)
          .deletedAt(LocalDateTime.now())
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(deletedComment));

      // when & then
      assertThatThrownBy(() -> commentService.deleteComment(commentId, userEmail))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("이미 삭제된 댓글입니다");
    }
  }

  @Nested
  @DisplayName("사용자 댓글 조회 (getUserComments)")
  class GetUserComments {

    @Test
    @DisplayName("성공: 사용자가 작성한 댓글 목록을 페이징으로 조회한다")
    void getUserComments_Success() {
      // given
      String userEmail = "testuser@example.com";
      Pageable pageable = PageRequest.of(0, 20);

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(1L, "테스트 스레드");
      Comment mockComment = createMockComment(1L, "내 댓글", mockUser, mockThread);
      Page<Comment> commentPage = new PageImpl<>(List.of(mockComment), pageable, 1);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByAuthorIdOrderByCreatedAtDesc(mockUser.getId(), pageable))
          .willReturn(commentPage);

      // when
      Page<CommentResponse> result = commentService.getUserComments(userEmail, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getContent()).isEqualTo("내 댓글");
      assertThat(result.getTotalElements()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("멘션된 댓글 조회 (getMentionedComments)")
  class GetMentionedComments {

    @Test
    @DisplayName("성공: 사용자가 멘션된 댓글 목록을 페이징으로 조회한다")
    void getMentionedComments_Success() {
      // given
      String userEmail = "testuser@example.com";
      Pageable pageable = PageRequest.of(0, 20);

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User otherUser = createMockUser(2L, "other@example.com", "other");
      Thread mockThread = createMockThread(1L, "테스트 스레드");
      Comment mockComment = createMockComment(1L, "@testuser 안녕하세요!", otherUser, mockThread);
      Page<Comment> commentPage = new PageImpl<>(List.of(mockComment), pageable, 1);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByMentionedUserId(mockUser.getId(), pageable))
          .willReturn(commentPage);

      // when
      Page<CommentResponse> result = commentService.getMentionedComments(userEmail, pageable);

      // then
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getContent()).isEqualTo("@testuser 안녕하세요!");
      assertThat(result.getTotalElements()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("댓글 개수 조회 (getCommentCount)")
  class GetCommentCount {

    @Test
    @DisplayName("성공: 스레드의 활성 댓글 개수를 조회한다")
    void getCommentCount_Success() {
      // given
      Long threadId = 1L;
      long expectedCount = 5L;

      given(commentRepository.countActiveByThreadId(threadId)).willReturn(expectedCount);

      // when
      long result = commentService.getCommentCount(threadId);

      // then
      assertThat(result).isEqualTo(expectedCount);
    }
  }
}