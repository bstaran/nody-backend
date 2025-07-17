package org.nodystudio.nodybackend.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.nodystudio.nodybackend.event.CommentMentionEvent;
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

    @Test
    @DisplayName("성공: 멘션이 포함된 댓글을 생성하고 이벤트를 발행한다")
    void createCommentWithMention_Success() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@mentioneduser 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User mentionedUser = createMockUser(2L, "mentioneduser@example.com", "mentioneduser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment mockComment = Comment.builder()
          .id(1L)
          .content(request.getContent())
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(Set.of(mentionedUser))
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("mentioneduser")).willReturn(
          Optional.of(mentionedUser));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      commentService.createComment(threadId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(
          CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getCommentId()).isEqualTo(1L);
      assertThat(event.getAuthorId()).isEqualTo(1L);
      assertThat(event.getThreadId()).isEqualTo(threadId);
      assertThat(event.getMentionedUserIds()).contains(2L);
    }

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

    @Test
    @DisplayName("성공: 존재하지 않는 사용자 멘션은 무시되고 댓글이 생성된다")
    void createComment_WithNonExistentMention_ShouldIgnoreAndCreateComment() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@nonexistentuser 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");
      Comment mockComment = createMockComment(1L, request.getContent(), mockUser, mockThread);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("nonexistentuser")).willReturn(Optional.empty());
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      CommentResponse result = commentService.createComment(threadId, request, userEmail);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("@nonexistentuser 안녕하세요!");

      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
    }

    @Test
    @DisplayName("성공: 비활성 사용자 멘션은 무시되고 댓글이 생성된다")
    void createComment_WithInactiveMention_ShouldIgnoreAndCreateComment() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@inactiveuser 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");
      Comment mockComment = createMockComment(1L, request.getContent(), mockUser, mockThread);

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("inactiveuser")).willReturn(Optional.empty());
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      CommentResponse result = commentService.createComment(threadId, request, userEmail);

      // then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("@inactiveuser 안녕하세요!");

      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
    }

    @Test
    @DisplayName("성공: 잘못된 멘션 형식은 무시되고 댓글이 생성된다")
    void createComment_WithInvalidMentionFormat_ShouldIgnoreAndCreateComment() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@ john (공백포함), @, @123 안녕하세요!")
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
      assertThat(result.getContent()).isEqualTo("@ john (공백포함), @, @123 안녕하세요!");

      // 이벤트가 발행되지 않았는지 확인 (유효한 멘션이 없으므로)
      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
    }

    @Test
    @DisplayName("성공: 중복 멘션은 한 번만 처리된다")
    void createComment_WithDuplicateMention_ShouldProcessOnlyOnce() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@john @john 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User mentionedUser = createMockUser(2L, "john@example.com", "john");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment mockComment = Comment.builder()
          .id(1L)
          .content(request.getContent())
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(Set.of(mentionedUser)) // Set이므로 중복 제거됨
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("john")).willReturn(Optional.of(mentionedUser));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      commentService.createComment(threadId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getMentionedUserIds()).hasSize(1); // 중복 제거되어 1개만
      assertThat(event.getMentionedUserIds()).contains(2L);
    }

    @Test
    @DisplayName("성공: 유효한 멘션과 무효한 멘션이 혼재할 때 유효한 것만 처리된다")
    void createComment_WithMixedValidInvalidMentions_ShouldProcessOnlyValid() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@john @nonexistent @alice 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User johnUser = createMockUser(2L, "john@example.com", "john");
      User aliceUser = createMockUser(3L, "alice@example.com", "alice");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment mockComment = Comment.builder()
          .id(1L)
          .content(request.getContent())
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(Set.of(johnUser, aliceUser)) // 유효한 사용자만
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("john")).willReturn(Optional.of(johnUser));
      given(userRepository.findByNicknameAndIsActiveTrue("nonexistent")).willReturn(Optional.empty());
      given(userRepository.findByNicknameAndIsActiveTrue("alice")).willReturn(Optional.of(aliceUser));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      commentService.createComment(threadId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getMentionedUserIds()).hasSize(2); // 유효한 2명만
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("성공: 여러 사용자 멘션 시 모든 사용자에게 이벤트가 발행된다")
    void createComment_WithMultipleMentions_ShouldPublishEventForAll() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@john @alice @bob 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User johnUser = createMockUser(2L, "john@example.com", "john");
      User aliceUser = createMockUser(3L, "alice@example.com", "alice");
      User bobUser = createMockUser(4L, "bob@example.com", "bob");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment mockComment = Comment.builder()
          .id(1L)
          .content(request.getContent())
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(Set.of(johnUser, aliceUser, bobUser))
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("john")).willReturn(Optional.of(johnUser));
      given(userRepository.findByNicknameAndIsActiveTrue("alice")).willReturn(Optional.of(aliceUser));
      given(userRepository.findByNicknameAndIsActiveTrue("bob")).willReturn(Optional.of(bobUser));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      commentService.createComment(threadId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getMentionedUserIds()).hasSize(3);
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @Test
    @DisplayName("성공: 멘션 없는 댓글은 이벤트가 발행되지 않는다")
    void createComment_WithoutMention_ShouldNotPublishEvent() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("안녕하세요! 멘션이 없는 댓글입니다.")
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
      assertThat(result.getContent()).isEqualTo("안녕하세요! 멘션이 없는 댓글입니다.");

      // 이벤트가 발행되지 않았는지 확인
      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
    }

    @Test
    @DisplayName("성공: 한글 닉네임 멘션이 정상 처리된다")
    void createComment_WithKoreanNickname_ShouldProcessCorrectly() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@홍길동 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User koreanUser = createMockUser(2L, "hong@example.com", "홍길동");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment mockComment = Comment.builder()
          .id(1L)
          .content(request.getContent())
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(Set.of(koreanUser))
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("홍길동")).willReturn(Optional.of(koreanUser));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      commentService.createComment(threadId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getMentionedUserIds()).contains(2L);
    }

    @Test
    @DisplayName("성공: 숫자로 시작하는 멘션은 무효한 멘션으로 무시된다")
    void createComment_WithNumericStartMention_ShouldIgnoreAndCreateComment() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@123username @456 @789abc 안녕하세요!")
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
      assertThat(result.getContent()).isEqualTo("@123username @456 @789abc 안녕하세요!");

      // 숫자로 시작하는 멘션은 모두 무효하므로 이벤트가 발행되지 않아야 함
      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
    }

    @Test
    @DisplayName("성공: 유효한 멘션과 숫자 시작 무효 멘션이 혼재할 때 유효한 것만 처리된다")
    void createComment_WithMixedValidAndNumericMentions_ShouldProcessOnlyValid() {
      // given
      Long threadId = 1L;
      String userEmail = "testuser@example.com";
      CommentCreateRequest request = CommentCreateRequest.builder()
          .content("@john @123 @alice @456def 안녕하세요!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User johnUser = createMockUser(2L, "john@example.com", "john");
      User aliceUser = createMockUser(3L, "alice@example.com", "alice");
      Thread mockThread = createMockThread(threadId, "테스트 스레드");

      Comment mockComment = Comment.builder()
          .id(1L)
          .content(request.getContent())
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(Set.of(johnUser, aliceUser)) // 유효한 사용자만
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(threadRepository.findById(threadId)).willReturn(Optional.of(mockThread));
      given(userRepository.findByNicknameAndIsActiveTrue("john")).willReturn(Optional.of(johnUser));
      given(userRepository.findByNicknameAndIsActiveTrue("alice")).willReturn(Optional.of(aliceUser));
      given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

      // when
      commentService.createComment(threadId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getMentionedUserIds()).hasSize(2); // john, alice만
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(2L, 3L);
      
      // 숫자로 시작하는 멘션에 대한 조회가 시도되지 않았는지 확인
      then(userRepository).should(never()).findByNicknameAndIsActiveTrue("123");
      then(userRepository).should(never()).findByNicknameAndIsActiveTrue("456def");
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
          .hasMessageContaining("댓글을 찾을 수 없습니다");
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

    @Test
    @DisplayName("성공: 새로운 멘션 추가 시 해당 사용자들에게만 이벤트가 발행된다")
    void updateComment_WithNewMentions_ShouldPublishEventOnlyForNewOnes() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("@alice @bob 새로운 멘션이 추가되었습니다!")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User aliceUser = createMockUser(2L, "alice@example.com", "alice");
      User bobUser = createMockUser(3L, "bob@example.com", "bob");
      User johnUser = createMockUser(4L, "john@example.com", "john");
      Thread mockThread = createMockThread(1L, "테스트 스레드");

      Comment existingComment = Comment.builder()
          .id(commentId)
          .content("@john 기존 댓글")
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(new HashSet<>(Set.of(johnUser)))
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(existingComment));
      given(userRepository.findByNicknameAndIsActiveTrue("alice")).willReturn(Optional.of(aliceUser));
      given(userRepository.findByNicknameAndIsActiveTrue("bob")).willReturn(Optional.of(bobUser));

      // when
      commentService.updateComment(commentId, request, userEmail);

      // then
      ArgumentCaptor<CommentMentionEvent> eventCaptor = ArgumentCaptor.forClass(CommentMentionEvent.class);
      then(eventPublisher).should().publishEvent(eventCaptor.capture());

      CommentMentionEvent event = eventCaptor.getValue();
      assertThat(event.getMentionedUserIds()).hasSize(2); // 새로 추가된 alice, bob만
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("성공: 기존 멘션만 있는 댓글 수정 시 이벤트가 발행되지 않는다")
    void updateComment_WithExistingMentionsOnly_ShouldNotPublishEvent() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("@john 기존 멘션만 있는 수정된 댓글")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User johnUser = createMockUser(2L, "john@example.com", "john");
      Thread mockThread = createMockThread(1L, "테스트 스레드");

      Comment existingComment = Comment.builder()
          .id(commentId)
          .content("@john 기존 댓글")
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(new HashSet<>(Set.of(johnUser)))
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(existingComment));
      given(userRepository.findByNicknameAndIsActiveTrue("john")).willReturn(Optional.of(johnUser));

      // when
      commentService.updateComment(commentId, request, userEmail);

      // then
      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
    }

    @Test
    @DisplayName("성공: 멘션 제거 시 이벤트가 발행되지 않는다")
    void updateComment_WithMentionRemoval_ShouldNotPublishEvent() {
      // given
      Long commentId = 1L;
      String userEmail = "testuser@example.com";
      CommentUpdateRequest request = CommentUpdateRequest.builder()
          .content("멘션이 제거된 댓글")
          .build();

      User mockUser = createMockUser(1L, userEmail, "testuser");
      User johnUser = createMockUser(2L, "john@example.com", "john");
      Thread mockThread = createMockThread(1L, "테스트 스레드");

      Comment existingComment = Comment.builder()
          .id(commentId)
          .content("@john 기존 댓글")
          .author(mockUser)
          .thread(mockThread)
          .mentionedUsers(new HashSet<>(Set.of(johnUser)))
          .build();

      given(userRepository.findByEmailAndIsActiveTrue(userEmail)).willReturn(Optional.of(mockUser));
      given(commentRepository.findByIdAndAuthorId(commentId, mockUser.getId()))
          .willReturn(Optional.of(existingComment));

      // when
      commentService.updateComment(commentId, request, userEmail);

      // then
      then(eventPublisher).should(never()).publishEvent(any(CommentMentionEvent.class));
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
          .hasMessageContaining("댓글을 찾을 수 없습니다");
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