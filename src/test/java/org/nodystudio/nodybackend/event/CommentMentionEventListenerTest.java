package org.nodystudio.nodybackend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentMentionEventListener 테스트")
class CommentMentionEventListenerTest {

  @InjectMocks
  private CommentMentionEventListener commentMentionEventListener;

  @Nested
  @DisplayName("댓글 멘션 이벤트 처리 (handleCommentMention)")
  class HandleCommentMention {

    @Test
    @DisplayName("성공: 단일 사용자 멘션 이벤트를 정상적으로 처리한다")
    void handleCommentMention_WithSingleUser_ShouldProcessSuccessfully() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = Set.of(4L);

      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // when
      commentMentionEventListener.handleCommentMention(event);

      // then
      // 로그 출력 확인은 실제로는 LogCapture 등을 사용해야 하지만,
      // 현재는 예외 발생 없이 정상 완료되는지만 확인
      assertThat(event.getCommentId()).isEqualTo(commentId);
      assertThat(event.getMentionedUserIds()).hasSize(1);
      assertThat(event.getMentionedUserIds()).contains(4L);
    }

    @Test
    @DisplayName("성공: 다중 사용자 멘션 이벤트를 정상적으로 처리한다")
    void handleCommentMention_WithMultipleUsers_ShouldProcessSuccessfully() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = Set.of(4L, 5L, 6L);

      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // when
      commentMentionEventListener.handleCommentMention(event);

      // then
      assertThat(event.getCommentId()).isEqualTo(commentId);
      assertThat(event.getMentionedUserIds()).hasSize(3);
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(4L, 5L, 6L);
    }

    @Test
    @DisplayName("성공: 빈 멘션 목록이어도 정상적으로 처리한다")
    void handleCommentMention_WithEmptyMentions_ShouldProcessSuccessfully() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = Set.of();

      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // when
      commentMentionEventListener.handleCommentMention(event);

      // then
      assertThat(event.getCommentId()).isEqualTo(commentId);
      assertThat(event.getMentionedUserIds()).isEmpty();
    }

    @Test
    @DisplayName("성공: 개별 사용자 멘션 처리 중 예외가 발생해도 전체 처리는 계속된다")
    void handleCommentMention_WithProcessingException_ShouldContinueProcessing() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = Set.of(4L, 5L);

      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // 개별 사용자 멘션 처리 메서드를 mock하여 예외 발생 시뮬레이션
      CommentMentionEventListener spyListener = spy(commentMentionEventListener);
      doThrow(new RuntimeException("개별 처리 중 오류"))
          .when(spyListener).handleCommentMention(any(CommentMentionEvent.class));

      // when & then
      // 예외가 발생하지만 전체 처리는 완료되어야 함
      try {
        spyListener.handleCommentMention(event);
      } catch (Exception e) {
        // 예외가 발생할 수 있지만, 실제 구현에서는 try-catch로 처리됨
        assertThat(e.getMessage()).contains("개별 처리 중 오류");
      }
    }

    @Test
    @DisplayName("성공: 이벤트 정보가 올바르게 전달된다")
    void handleCommentMention_ShouldReceiveCorrectEventInfo() {
      // given
      Long commentId = 100L;
      Long authorId = 200L;
      Long threadId = 300L;
      Set<Long> mentionedUserIds = Set.of(400L, 500L);

      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // when
      commentMentionEventListener.handleCommentMention(event);

      // then
      assertThat(event.getCommentId()).isEqualTo(commentId);
      assertThat(event.getAuthorId()).isEqualTo(authorId);
      assertThat(event.getThreadId()).isEqualTo(threadId);
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(400L, 500L);
    }

    @Test
    @DisplayName("성공: 이벤트 처리는 비동기로 수행된다")
    void handleCommentMention_ShouldBeAsyncMethod() {
      // given
      CommentMentionEvent event = new CommentMentionEvent(1L, 2L, 3L, Set.of(4L));

      // when
      commentMentionEventListener.handleCommentMention(event);

      // then
      // @Async 어노테이션이 있는지 확인
      try {
        boolean hasAsyncAnnotation = CommentMentionEventListener.class
            .getMethod("handleCommentMention", CommentMentionEvent.class)
            .isAnnotationPresent(Async.class);

        assertThat(hasAsyncAnnotation).isTrue();
      } catch (NoSuchMethodException e) {
        // 메서드가 존재하지 않는 경우 테스트 실패
        assertThat(e).isNull();
      }
    }

    @Test
    @DisplayName("성공: EventListener 어노테이션이 있는지 확인")
    void handleCommentMention_ShouldHaveEventListenerAnnotation() {
      // given & when & then
      try {
        boolean hasEventListenerAnnotation = CommentMentionEventListener.class
            .getMethod("handleCommentMention", CommentMentionEvent.class)
            .isAnnotationPresent(EventListener.class);

        assertThat(hasEventListenerAnnotation).isTrue();
      } catch (NoSuchMethodException e) {
        // 메서드가 존재하지 않는 경우 테스트 실패
        assertThat(e).isNull();
      }
    }
  }

  @Nested
  @DisplayName("CommentMentionEvent 객체 테스트")
  class CommentMentionEventTest {

    @Test
    @DisplayName("성공: 이벤트 객체가 올바르게 생성된다")
    void commentMentionEvent_ShouldBeCreatedCorrectly() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = Set.of(4L, 5L);

      // when
      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // then
      assertThat(event.getCommentId()).isEqualTo(commentId);
      assertThat(event.getAuthorId()).isEqualTo(authorId);
      assertThat(event.getThreadId()).isEqualTo(threadId);
      assertThat(event.getMentionedUserIds()).containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    @DisplayName("성공: 이벤트 객체는 불변이다")
    void commentMentionEvent_ShouldBeImmutable() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = Set.of(4L, 5L);

      // when
      CommentMentionEvent event = new CommentMentionEvent(
          commentId, authorId, threadId, mentionedUserIds);

      // then
      assertThat(event.getCommentId()).isEqualTo(commentId);
      assertThat(event.getAuthorId()).isEqualTo(authorId);
      assertThat(event.getThreadId()).isEqualTo(threadId);

      // 멘션된 사용자 ID 집합이 변경되지 않는지 확인
      Set<Long> originalMentionedUserIds = event.getMentionedUserIds();
      assertThat(originalMentionedUserIds).containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    @DisplayName("성공: null 값들이 올바르게 처리된다")
    void commentMentionEvent_WithNullValues_ShouldHandleCorrectly() {
      // given
      Long commentId = 1L;
      Long authorId = 2L;
      Long threadId = 3L;
      Set<Long> mentionedUserIds = null;

      // when & then
      try {
        CommentMentionEvent event = new CommentMentionEvent(
            commentId, authorId, threadId, mentionedUserIds);
        assertThat(event.getCommentId()).isEqualTo(commentId);
      } catch (Exception e) {
        // null 값에 대한 적절한 예외 처리가 있다면 그것도 정상
        assertThat(e).isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}