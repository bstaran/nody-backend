package org.nodystudio.nodybackend.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;

@DisplayName("Comment Entity 테스트")
class CommentTest {

  private User testUser1;
  private User testUser2;
  private Thread testThread;

  @BeforeEach
  void setUp() {
    testUser1 = User.builder()
        .id(1L)
        .provider(org.nodystudio.nodybackend.domain.enums.OAuthProvider.GOOGLE)
        .socialId("test-social-id-1")
        .email("user1@example.com")
        .nickname("user1")
        .isActive(true)
        .build();

    testUser2 = User.builder()
        .id(2L)
        .provider(org.nodystudio.nodybackend.domain.enums.OAuthProvider.GOOGLE)
        .socialId("test-social-id-2")
        .email("user2@example.com")
        .nickname("user2")
        .isActive(true)
        .build();

    testThread = Thread.builder()
        .id(1L)
        .content("테스트 스레드")
        .user(testUser1)
        .build();
  }

  @Nested
  @DisplayName("댓글 생성 테스트")
  class CommentCreation {

    @Test
    @DisplayName("성공: 빌더 패턴으로 댓글을 생성한다")
    void createComment_Success() {
      // given & when
      Comment comment = Comment.builder()
          .content("테스트 댓글입니다.")
          .author(testUser1)
          .thread(testThread)
          .build();

      // then
      assertThat(comment.getContent()).isEqualTo("테스트 댓글입니다.");
      assertThat(comment.getAuthor()).isEqualTo(testUser1);
      assertThat(comment.getThread()).isEqualTo(testThread);
      assertThat(comment.getMentionedUsers()).isEmpty();
      assertThat(comment.getChildren()).isEmpty();
      assertThat(comment.getParent()).isNull();
      assertThat(comment.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("성공: 대댓글을 생성한다")
    void createReplyComment_Success() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      Comment replyComment = Comment.builder()
          .content("대댓글")
          .author(testUser2)
          .thread(testThread)
          .parent(parentComment)
          .build();

      // then
      assertThat(replyComment.getParent()).isEqualTo(parentComment);
      assertThat(replyComment.isReply()).isTrue();
      assertThat(replyComment.isRootComment()).isFalse();
    }

    @Test
    @DisplayName("성공: 멘션이 포함된 댓글을 생성한다")
    void createCommentWithMentions_Success() {
      // given
      Set<User> mentionedUsers = new HashSet<>();
      mentionedUsers.add(testUser2);

      // when
      Comment comment = Comment.builder()
          .content("@user2 안녕하세요!")
          .author(testUser1)
          .thread(testThread)
          .mentionedUsers(mentionedUsers)
          .build();

      // then
      assertThat(comment.getMentionedUsers()).hasSize(1);
      assertThat(comment.getMentionedUsers()).contains(testUser2);
    }
  }

  @Nested
  @DisplayName("댓글 내용 수정 테스트")
  class UpdateContent {

    @Test
    @DisplayName("성공: 댓글 내용을 수정한다")
    void updateContent_Success() {
      // given
      Comment comment = Comment.builder()
          .content("원본 내용")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      comment.updateContent("수정된 내용");

      // then
      assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("성공: 앞뒤 공백이 제거된다")
    void updateContent_TrimsWhitespace() {
      // given
      Comment comment = Comment.builder()
          .content("원본 내용")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      comment.updateContent("  수정된 내용  ");

      // then
      assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("실패: null 내용으로 수정하면 예외가 발생한다")
    void updateContent_Failure_NullContent() {
      // given
      Comment comment = Comment.builder()
          .content("원본 내용")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when & then
      assertThatThrownBy(() -> comment.updateContent(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("내용은 공백일 수 없습니다");
    }

    @Test
    @DisplayName("실패: 빈 내용으로 수정하면 예외가 발생한다")
    void updateContent_Failure_EmptyContent() {
      // given
      Comment comment = Comment.builder()
          .content("원본 내용")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when & then
      assertThatThrownBy(() -> comment.updateContent(""))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("내용은 공백일 수 없습니다");
    }

    @Test
    @DisplayName("실패: 공백만 있는 내용으로 수정하면 예외가 발생한다")
    void updateContent_Failure_WhitespaceOnlyContent() {
      // given
      Comment comment = Comment.builder()
          .content("원본 내용")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when & then
      assertThatThrownBy(() -> comment.updateContent("   "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("내용은 공백일 수 없습니다");
    }
  }

  @Nested
  @DisplayName("멘션 사용자 관리 테스트")
  class MentionManagement {

    @Test
    @DisplayName("성공: 멘션 사용자를 추가한다")
    void addMentionedUser_Success() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      comment.addMentionedUser(testUser2);

      // then
      assertThat(comment.getMentionedUsers()).hasSize(1);
      assertThat(comment.getMentionedUsers()).contains(testUser2);
    }

    @Test
    @DisplayName("성공: null 사용자를 추가해도 예외가 발생하지 않는다")
    void addMentionedUser_Success_NullUser() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      comment.addMentionedUser(null);

      // then
      assertThat(comment.getMentionedUsers()).isEmpty();
    }

    @Test
    @DisplayName("성공: 멘션 사용자 목록을 설정한다")
    void setMentionedUsers_Success() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      Set<User> newMentions = new HashSet<>();
      newMentions.add(testUser2);

      // when
      comment.setMentionedUsers(newMentions);

      // then
      assertThat(comment.getMentionedUsers()).hasSize(1);
      assertThat(comment.getMentionedUsers()).contains(testUser2);
    }

    @Test
    @DisplayName("성공: 기존 멘션을 새로운 목록으로 교체한다")
    void setMentionedUsers_Success_ReplaceExisting() {
      // given
      Set<User> initialMentions = new HashSet<>();
      initialMentions.add(testUser2);

      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .mentionedUsers(initialMentions)
          .build();

      User newUser = User.builder()
          .id(3L)
          .provider(org.nodystudio.nodybackend.domain.enums.OAuthProvider.GOOGLE)
          .socialId("test-social-id-3")
          .email("user3@example.com")
          .nickname("user3")
          .build();

      Set<User> newMentions = new HashSet<>();
      newMentions.add(newUser);

      // when
      comment.setMentionedUsers(newMentions);

      // then
      assertThat(comment.getMentionedUsers()).hasSize(1);
      assertThat(comment.getMentionedUsers()).contains(newUser);
      assertThat(comment.getMentionedUsers()).doesNotContain(testUser2);
    }

    @Test
    @DisplayName("성공: null 목록으로 설정하면 기존 멘션이 모두 제거된다")
    void setMentionedUsers_Success_NullList() {
      // given
      Set<User> initialMentions = new HashSet<>();
      initialMentions.add(testUser2);

      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .mentionedUsers(initialMentions)
          .build();

      // when
      comment.setMentionedUsers(null);

      // then
      assertThat(comment.getMentionedUsers()).isEmpty();
    }
  }

  @Nested
  @DisplayName("소유권 확인 테스트")
  class OwnershipCheck {

    @Test
    @DisplayName("성공: 작성자가 맞으면 true를 반환한다")
    void isOwnedBy_Success_SameUser() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      boolean isOwned = comment.isOwnedBy(testUser1);

      // then
      assertThat(isOwned).isTrue();
    }

    @Test
    @DisplayName("성공: 작성자가 아니면 false를 반환한다")
    void isOwnedBy_Success_DifferentUser() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      boolean isOwned = comment.isOwnedBy(testUser2);

      // then
      assertThat(isOwned).isFalse();
    }

    @Test
    @DisplayName("성공: null 사용자면 false를 반환한다")
    void isOwnedBy_Success_NullUser() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      boolean isOwned = comment.isOwnedBy(null);

      // then
      assertThat(isOwned).isFalse();
    }

    @Test
    @DisplayName("성공: 작성자가 null이면 false를 반환한다")
    void isOwnedBy_Success_NullAuthor() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(null)
          .thread(testThread)
          .build();

      // when
      boolean isOwned = comment.isOwnedBy(testUser1);

      // then
      assertThat(isOwned).isFalse();
    }
  }

  @Nested
  @DisplayName("삭제 상태 확인 테스트")
  class DeletionCheck {

    @Test
    @DisplayName("성공: 삭제되지 않은 댓글은 false를 반환한다")
    void isDeleted_Success_NotDeleted() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      boolean isDeleted = comment.isDeleted();

      // then
      assertThat(isDeleted).isFalse();
    }

    @Test
    @DisplayName("성공: 삭제된 댓글은 true를 반환한다")
    void isDeleted_Success_Deleted() {
      // given
      Comment comment = Comment.builder()
          .content("댓글")
          .author(testUser1)
          .thread(testThread)
          .deletedAt(LocalDateTime.now())
          .build();

      // when
      boolean isDeleted = comment.isDeleted();

      // then
      assertThat(isDeleted).isTrue();
    }
  }

  @Nested
  @DisplayName("댓글 유형 확인 테스트")
  class CommentTypeCheck {

    @Test
    @DisplayName("성공: 부모가 없으면 최상위 댓글로 판단한다")
    void isRootComment_Success_NoParent() {
      // given
      Comment comment = Comment.builder()
          .content("최상위 댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when & then
      assertThat(comment.isRootComment()).isTrue();
      assertThat(comment.isReply()).isFalse();
    }

    @Test
    @DisplayName("성공: 부모가 있으면 대댓글로 판단한다")
    void isReply_Success_HasParent() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      Comment replyComment = Comment.builder()
          .content("대댓글")
          .author(testUser2)
          .thread(testThread)
          .parent(parentComment)
          .build();

      // when & then
      assertThat(replyComment.isReply()).isTrue();
      assertThat(replyComment.isRootComment()).isFalse();
    }
  }

  @Nested
  @DisplayName("연관관계 편의 메서드 테스트")
  class RelationshipConvenience {

    @Test
    @DisplayName("성공: 자식 댓글을 추가한다")
    void addChild_Success() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      Comment childComment = Comment.builder()
          .content("자식 댓글")
          .author(testUser2)
          .thread(testThread)
          .build();

      // when
      parentComment.addChild(childComment);

      // then
      assertThat(parentComment.getChildren()).hasSize(1);
      assertThat(parentComment.getChildren()).contains(childComment);
      assertThat(childComment.getParent()).isEqualTo(parentComment);
    }

    @Test
    @DisplayName("성공: 자식 댓글을 제거한다")
    void removeChild_Success() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      Comment childComment = Comment.builder()
          .content("자식 댓글")
          .author(testUser2)
          .thread(testThread)
          .parent(parentComment)
          .build();

      parentComment.addChild(childComment);

      // when
      parentComment.removeChild(childComment);

      // then
      assertThat(parentComment.getChildren()).isEmpty();
      assertThat(childComment.getParent()).isNull();
    }

    @Test
    @DisplayName("성공: 여러 자식 댓글을 관리한다")
    void multipleChildren_Success() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread)
          .build();

      Comment childComment1 = Comment.builder()
          .content("자식 댓글 1")
          .author(testUser2)
          .thread(testThread)
          .build();

      Comment childComment2 = Comment.builder()
          .content("자식 댓글 2")
          .author(testUser1)
          .thread(testThread)
          .build();

      // when
      parentComment.addChild(childComment1);
      parentComment.addChild(childComment2);

      // then
      assertThat(parentComment.getChildren()).hasSize(2);
      assertThat(parentComment.getChildren()).containsExactlyInAnyOrder(childComment1,
          childComment2);
      assertThat(childComment1.getParent()).isEqualTo(parentComment);
      assertThat(childComment2.getParent()).isEqualTo(parentComment);
    }
  }
}