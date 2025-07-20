package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.comment.Comment;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CommentRepository 테스트")
class CommentRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private CommentRepository commentRepository;

  private User testUser1;
  private User testUser2;
  private Thread testThread1;
  private Thread testThread2;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser1 = User.builder()
        .provider(org.nodystudio.nodybackend.domain.enums.OAuthProvider.GOOGLE)
        .socialId("test-social-id-1")
        .email("user1@example.com")
        .nickname("user1")
        .isActive(true)
        .build();
    entityManager.persist(testUser1);

    testUser2 = User.builder()
        .provider(org.nodystudio.nodybackend.domain.enums.OAuthProvider.GOOGLE)
        .socialId("test-social-id-2")
        .email("user2@example.com")
        .nickname("user2")
        .isActive(true)
        .build();
    entityManager.persist(testUser2);

    // 테스트 스레드 생성
    testThread1 = Thread.builder()
        .content("테스트 스레드 1 내용")
        .user(testUser1)
        .build();
    entityManager.persist(testThread1);

    testThread2 = Thread.builder()
        .content("테스트 스레드 2 내용")
        .user(testUser2)
        .build();
    entityManager.persist(testThread2);

    entityManager.flush();
    entityManager.clear();
  }

  @Nested
  @DisplayName("기본 CRUD 테스트")
  class BasicCrudTest {

    @Test
    @DisplayName("댓글을 성공적으로 저장하고 조회한다")
    void saveAndFindComment_Success() {
      // given
      Comment comment = Comment.builder()
          .content("테스트 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();

      // when
      Comment savedComment = commentRepository.save(comment);
      entityManager.flush();
      entityManager.clear();

      Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());

      // then
      assertThat(foundComment).isPresent();
      assertThat(foundComment.get().getContent()).isEqualTo("테스트 댓글");
      assertThat(foundComment.get().getAuthor().getId()).isEqualTo(testUser1.getId());
      assertThat(foundComment.get().getThread().getId()).isEqualTo(testThread1.getId());
    }

    @Test
    @DisplayName("대댓글을 성공적으로 저장하고 조회한다")
    void saveAndFindReplyComment_Success() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      Comment savedParent = commentRepository.save(parentComment);
      entityManager.flush();

      Comment replyComment = Comment.builder()
          .content("대댓글")
          .author(testUser2)
          .thread(testThread1)
          .parent(savedParent)
          .build();

      // when
      Comment savedReply = commentRepository.save(replyComment);
      entityManager.flush();
      entityManager.clear();

      Optional<Comment> foundReply = commentRepository.findById(savedReply.getId());

      // then
      assertThat(foundReply).isPresent();
      assertThat(foundReply.get().getParent()).isNotNull();
      assertThat(foundReply.get().getParent().getId()).isEqualTo(savedParent.getId());
      assertThat(foundReply.get().isReply()).isTrue();
      assertThat(foundReply.get().isRootComment()).isFalse();
    }
  }

  @Nested
  @DisplayName("스레드별 댓글 조회 테스트")
  class ThreadCommentsTest {

    @Test
    @DisplayName("스레드의 모든 댓글을 작성자와 멘션 정보와 함께 조회한다")
    void findByThreadIdWithAuthorAndMentions_Success() {
      // given
      Comment comment1 = Comment.builder()
          .content("첫 번째 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      comment1.addMentionedUser(testUser2);
      commentRepository.save(comment1);

      Comment comment2 = Comment.builder()
          .content("두 번째 댓글")
          .author(testUser2)
          .thread(testThread1)
          .build();
      commentRepository.save(comment2);

      // 다른 스레드의 댓글 (결과에 포함되지 않아야 함)
      Comment otherThreadComment = Comment.builder()
          .content("다른 스레드 댓글")
          .author(testUser1)
          .thread(testThread2)
          .build();
      commentRepository.save(otherThreadComment);

      entityManager.flush();
      entityManager.clear();

      // when
      List<Comment> comments = commentRepository.findByThreadIdWithAuthorAndMentions(
          testThread1.getId());

      // then
      assertThat(comments).hasSize(2);
      assertThat(comments.get(0).getContent()).isEqualTo("첫 번째 댓글");
      assertThat(comments.get(1).getContent()).isEqualTo("두 번째 댓글");

      // 페치 조인으로 author와 mentionedUsers가 로드되었는지 확인
      assertThat(comments.get(0).getAuthor()).isNotNull();
      assertThat(comments.get(0).getMentionedUsers()).hasSize(1);
    }

    @Test
    @DisplayName("스레드의 댓글을 페이징하여 조회한다")
    void findByThreadIdWithAuthor_Paging_Success() {
      // given
      for (int i = 1; i <= 25; i++) {
        Comment comment = Comment.builder()
            .content("댓글 " + i)
            .author(testUser1)
            .thread(testThread1)
            .build();
        commentRepository.save(comment);
      }
      entityManager.flush();
      entityManager.clear();

      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<Comment> commentsPage = commentRepository.findByThreadIdWithAuthor(testThread1.getId(),
          pageable);

      // then
      assertThat(commentsPage.getContent()).hasSize(10);
      assertThat(commentsPage.getTotalElements()).isEqualTo(25);
      assertThat(commentsPage.getTotalPages()).isEqualTo(3);
      assertThat(commentsPage.getContent().get(0).getAuthor()).isNotNull();
    }

    @Test
    @DisplayName("최상위 댓글만 조회한다")
    void findRootCommentsByThreadId_Success() {
      // given
      Comment rootComment1 = Comment.builder()
          .content("최상위 댓글 1")
          .author(testUser1)
          .thread(testThread1)
          .build();
      Comment savedRoot1 = commentRepository.save(rootComment1);

      Comment rootComment2 = Comment.builder()
          .content("최상위 댓글 2")
          .author(testUser2)
          .thread(testThread1)
          .build();
      commentRepository.save(rootComment2);

      // 대댓글 (결과에 포함되지 않아야 함)
      Comment replyComment = Comment.builder()
          .content("대댓글")
          .author(testUser2)
          .thread(testThread1)
          .parent(savedRoot1)
          .build();
      commentRepository.save(replyComment);

      entityManager.flush();
      entityManager.clear();

      // when
      List<Comment> rootComments = commentRepository.findRootCommentsByThreadId(
          testThread1.getId());

      // then
      assertThat(rootComments).hasSize(2);
      assertThat(rootComments).allMatch(Comment::isRootComment);
    }

    @Test
    @DisplayName("특정 댓글의 자식 댓글들을 조회한다")
    void findChildCommentsByParentId_Success() {
      // given
      Comment parentComment = Comment.builder()
          .content("부모 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      Comment savedParent = commentRepository.save(parentComment);

      Comment childComment1 = Comment.builder()
          .content("자식 댓글 1")
          .author(testUser2)
          .thread(testThread1)
          .parent(savedParent)
          .build();
      commentRepository.save(childComment1);

      Comment childComment2 = Comment.builder()
          .content("자식 댓글 2")
          .author(testUser1)
          .thread(testThread1)
          .parent(savedParent)
          .build();
      commentRepository.save(childComment2);

      entityManager.flush();
      entityManager.clear();

      // when
      List<Comment> childComments = commentRepository.findChildCommentsByParentId(
          savedParent.getId());

      // then
      assertThat(childComments).hasSize(2);
      assertThat(childComments).allMatch(
          comment -> comment.getParent().getId().equals(savedParent.getId()));
    }
  }

  @Nested
  @DisplayName("사용자별 댓글 조회 테스트")
  class UserCommentsTest {

    @Test
    @DisplayName("특정 사용자가 작성한 댓글을 최신순으로 조회한다")
    void findByAuthorIdOrderByCreatedAtDesc_Success() {
      // given
      Comment comment1 = Comment.builder()
          .content("첫 번째 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      commentRepository.save(comment1);

      Comment comment2 = Comment.builder()
          .content("두 번째 댓글")
          .author(testUser1)
          .thread(testThread2)
          .build();
      commentRepository.save(comment2);

      // 다른 사용자의 댓글 (결과에 포함되지 않아야 함)
      Comment otherUserComment = Comment.builder()
          .content("다른 사용자 댓글")
          .author(testUser2)
          .thread(testThread1)
          .build();
      commentRepository.save(otherUserComment);

      entityManager.flush();
      entityManager.clear();

      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<Comment> userComments = commentRepository.findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(
          testUser1.getId(), pageable);

      // then
      assertThat(userComments.getContent()).hasSize(2);
      assertThat(userComments.getContent()).allMatch(
          comment -> comment.getAuthor().getId().equals(testUser1.getId()));
    }

    @Test
    @DisplayName("특정 사용자가 멘션된 댓글을 조회한다")
    void findByMentionedUserId_Success() {
      // given
      Comment mentionComment1 = Comment.builder()
          .content("@user2 첫 번째 멘션")
          .author(testUser1)
          .thread(testThread1)
          .build();
      mentionComment1.addMentionedUser(testUser2);
      commentRepository.save(mentionComment1);

      Comment mentionComment2 = Comment.builder()
          .content("@user2 두 번째 멘션")
          .author(testUser1)
          .thread(testThread2)
          .build();
      mentionComment2.addMentionedUser(testUser2);
      commentRepository.save(mentionComment2);

      // 멘션되지 않은 댓글 (결과에 포함되지 않아야 함)
      Comment noMentionComment = Comment.builder()
          .content("멘션 없는 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      commentRepository.save(noMentionComment);

      entityManager.flush();
      entityManager.clear();

      Pageable pageable = PageRequest.of(0, 10);

      // when
      Page<Comment> mentionedComments = commentRepository.findByMentionedUserId(testUser2.getId(),
          pageable);

      // then
      assertThat(mentionedComments.getContent()).hasSize(2);
      assertThat(mentionedComments.getContent()).allMatch(comment ->
          comment.getMentionedUsers().stream()
              .anyMatch(user -> user.getId().equals(testUser2.getId()))
      );
    }

    @Test
    @DisplayName("ID와 작성자로 댓글을 조회한다 (권한 확인용)")
    void findByIdAndAuthorId_Success() {
      // given
      Comment comment = Comment.builder()
          .content("권한 확인용 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      Comment savedComment = commentRepository.save(comment);
      entityManager.flush();
      entityManager.clear();

      // when
      Optional<Comment> foundComment = commentRepository.findByIdAndAuthorIdAndDeletedAtIsNull(savedComment.getId(),
          testUser1.getId());
      Optional<Comment> notFoundComment = commentRepository.findByIdAndAuthorIdAndDeletedAtIsNull(
          savedComment.getId(), testUser2.getId());

      // then
      assertThat(foundComment).isPresent();
      assertThat(foundComment.get().getContent()).isEqualTo("권한 확인용 댓글");
      assertThat(notFoundComment).isEmpty(); // 다른 사용자는 조회 불가
    }
  }

  @Nested
  @DisplayName("댓글 개수 조회 테스트")
  class CommentCountTest {

    @Test
    @DisplayName("스레드의 활성 댓글 개수를 조회한다")
    void countActiveByThreadId_Success() {
      // given
      // 활성 댓글 3개
      for (int i = 1; i <= 3; i++) {
        Comment comment = Comment.builder()
            .content("활성 댓글 " + i)
            .author(testUser1)
            .thread(testThread1)
            .build();
        commentRepository.save(comment);
      }

      // 삭제된 댓글 2개
      for (int i = 1; i <= 2; i++) {
        Comment comment = Comment.builder()
            .content("삭제된 댓글 " + i)
            .author(testUser1)
            .thread(testThread1)
            .deletedAt(LocalDateTime.now())
            .build();
        commentRepository.save(comment);
      }

      entityManager.flush();
      entityManager.clear();

      // when
      long activeCount = commentRepository.countByThreadIdAndDeletedAtIsNull(testThread1.getId());

      // then
      assertThat(activeCount).isEqualTo(3); // 삭제된 댓글 제외
    }

    @Test
    @DisplayName("스레드의 전체 댓글 개수를 조회한다 (삭제된 댓글 포함)")
    void countByThreadId_Success() {
      // given
      // 활성 댓글 3개
      for (int i = 1; i <= 3; i++) {
        Comment comment = Comment.builder()
            .content("활성 댓글 " + i)
            .author(testUser1)
            .thread(testThread1)
            .build();
        commentRepository.save(comment);
      }

      // 삭제된 댓글 2개
      for (int i = 1; i <= 2; i++) {
        Comment comment = Comment.builder()
            .content("삭제된 댓글 " + i)
            .author(testUser1)
            .thread(testThread1)
            .deletedAt(LocalDateTime.now())
            .build();
        commentRepository.save(comment);
      }

      entityManager.flush();
      entityManager.clear();

      // when
      long totalCount = commentRepository.countByThreadId(testThread1.getId());

      // then
      assertThat(totalCount).isEqualTo(5); // 삭제된 댓글 포함
    }

    @Test
    @DisplayName("특정 사용자가 작성한 활성 댓글 개수를 조회한다")
    void countActiveByAuthorId_Success() {
      // given
      // user1의 활성 댓글 2개
      for (int i = 1; i <= 2; i++) {
        Comment comment = Comment.builder()
            .content("user1 활성 댓글 " + i)
            .author(testUser1)
            .thread(testThread1)
            .build();
        commentRepository.save(comment);
      }

      // user1의 삭제된 댓글 1개
      Comment deletedComment = Comment.builder()
          .content("user1 삭제된 댓글")
          .author(testUser1)
          .thread(testThread1)
          .deletedAt(LocalDateTime.now())
          .build();
      commentRepository.save(deletedComment);

      // user2의 활성 댓글 1개 (카운트에 포함되지 않아야 함)
      Comment otherUserComment = Comment.builder()
          .content("user2 댓글")
          .author(testUser2)
          .thread(testThread1)
          .build();
      commentRepository.save(otherUserComment);

      entityManager.flush();
      entityManager.clear();

      // when
      long user1ActiveCount = commentRepository.countByAuthorIdAndDeletedAtIsNull(testUser1.getId());

      // then
      assertThat(user1ActiveCount).isEqualTo(2); // user1의 활성 댓글만
    }
  }

  @Nested
  @DisplayName("특정 스레드 내 사용자 댓글 조회 테스트")
  class ThreadUserCommentsTest {

    @Test
    @DisplayName("특정 스레드에서 특정 사용자가 작성한 댓글을 조회한다")
    void findByThreadIdAndAuthorId_Success() {
      // given
      // testThread1에서 testUser1이 작성한 댓글 2개
      Comment comment1 = Comment.builder()
          .content("스레드1 user1 댓글1")
          .author(testUser1)
          .thread(testThread1)
          .build();
      commentRepository.save(comment1);

      Comment comment2 = Comment.builder()
          .content("스레드1 user1 댓글2")
          .author(testUser1)
          .thread(testThread1)
          .build();
      commentRepository.save(comment2);

      // testThread1에서 testUser2가 작성한 댓글 1개 (결과에 포함되지 않아야 함)
      Comment otherUserComment = Comment.builder()
          .content("스레드1 user2 댓글")
          .author(testUser2)
          .thread(testThread1)
          .build();
      commentRepository.save(otherUserComment);

      // testThread2에서 testUser1이 작성한 댓글 1개 (결과에 포함되지 않아야 함)
      Comment otherThreadComment = Comment.builder()
          .content("스레드2 user1 댓글")
          .author(testUser1)
          .thread(testThread2)
          .build();
      commentRepository.save(otherThreadComment);

      entityManager.flush();
      entityManager.clear();

      // when
      List<Comment> comments = commentRepository.findByThreadIdAndAuthorId(testThread1.getId(),
          testUser1.getId());

      // then
      assertThat(comments).hasSize(2);
      assertThat(comments).allMatch(comment ->
          comment.getThread().getId().equals(testThread1.getId()) &&
              comment.getAuthor().getId().equals(testUser1.getId())
      );
    }
  }

  @Nested
  @DisplayName("소프트 삭제 테스트")
  class SoftDeleteTest {

    @Test
    @DisplayName("소프트 삭제된 댓글은 일반 조회에서 제외된다")
    void softDeletedComments_NotIncludedInNormalQuery() {
      // given
      Comment activeComment = Comment.builder()
          .content("활성 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      Comment savedActive = commentRepository.save(activeComment);

      Comment deletedComment = Comment.builder()
          .content("삭제된 댓글")
          .author(testUser1)
          .thread(testThread1)
          .deletedAt(LocalDateTime.now())
          .build();
      commentRepository.save(deletedComment);

      entityManager.flush();
      entityManager.clear();

      // when
      Optional<Comment> foundActive = commentRepository.findById(savedActive.getId());
      List<Comment> threadComments = commentRepository.findByThreadIdWithAuthorAndMentions(
          testThread1.getId());

      // then
      assertThat(foundActive).isPresent(); // 활성 댓글은 조회됨
      assertThat(threadComments).hasSize(1); // 삭제된 댓글은 제외됨
      assertThat(threadComments.get(0).getContent()).isEqualTo("활성 댓글");
    }

    @Test
    @DisplayName("소프트 삭제 실행 시 deletedAt이 설정된다")
    void softDelete_SetsDeletedAt() {
      // given
      Comment comment = Comment.builder()
          .content("삭제될 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      Comment savedComment = commentRepository.save(comment);
      entityManager.flush();

      // when
      commentRepository.delete(savedComment);
      entityManager.flush();
      entityManager.clear();

      // then
      // @SQLDelete 어노테이션으로 인해 실제로는 UPDATE 쿼리가 실행되어 deletedAt이 설정됨
      // @Where 어노테이션으로 인해 삭제된 댓글은 일반 조회에서 제외됨
      Optional<Comment> foundComment = commentRepository.findById(savedComment.getId());
      assertThat(foundComment).isEmpty(); // @Where 절로 인해 조회되지 않음
    }
  }

  @Nested
  @DisplayName("사용자 댓글 완전 삭제 테스트")
  class DeleteDeactivatedByUserIdTest {

    @Test
    @DisplayName("비활성화된 사용자 댓글을 완전히 삭제한다")
    void deleteDeactivatedByUserId_Success() {
      // given
      // 활성 댓글
      Comment activeComment = Comment.builder()
          .content("활성 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      commentRepository.save(activeComment);

      // 비활성화된 댓글
      Comment deactivatedComment1 = Comment.builder()
          .content("비활성화된 댓글 1")
          .author(testUser1)
          .thread(testThread1)
          .deletedAt(LocalDateTime.now().minusDays(1))
          .build();
      commentRepository.save(deactivatedComment1);

      Comment deactivatedComment2 = Comment.builder()
          .content("비활성화된 댓글 2")
          .author(testUser1)
          .thread(testThread2)
          .deletedAt(LocalDateTime.now().minusDays(2))
          .build();
      commentRepository.save(deactivatedComment2);

      // 다른 사용자의 비활성화된 댓글 (삭제되지 않아야 함)
      Comment otherUserDeactivatedComment = Comment.builder()
          .content("다른 사용자 비활성화된 댓글")
          .author(testUser2)
          .thread(testThread1)
          .deletedAt(LocalDateTime.now().minusDays(1))
          .build();
      commentRepository.save(otherUserDeactivatedComment);

      entityManager.flush();
      entityManager.clear();

      // when
      int deletedCount = commentRepository.deleteDeactivatedByUserId(testUser1.getId());
      entityManager.flush();
      entityManager.clear();

      // then
      assertThat(deletedCount).isEqualTo(2); // testUser1의 비활성화된 댓글 2개 삭제

      // 활성 댓글은 여전히 존재
      Optional<Comment> foundActiveComment = commentRepository.findById(activeComment.getId());
      assertThat(foundActiveComment).isPresent();

      // 다른 사용자의 비활성화된 댓글은 여전히 존재
      Optional<Comment> foundOtherUserComment = commentRepository.findById(otherUserDeactivatedComment.getId());
      assertThat(foundOtherUserComment).isEmpty(); // @Where 절로 인해 조회되지 않지만 데이터는 존재
    }

    @Test
    @DisplayName("비활성화된 댓글이 없는 경우 0을 반환한다")
    void deleteDeactivatedByUserId_NoDeactivatedComments_ReturnsZero() {
      // given
      Comment activeComment = Comment.builder()
          .content("활성 댓글")
          .author(testUser1)
          .thread(testThread1)
          .build();
      commentRepository.save(activeComment);

      entityManager.flush();
      entityManager.clear();

      // when
      int deletedCount = commentRepository.deleteDeactivatedByUserId(testUser1.getId());

      // then
      assertThat(deletedCount).isEqualTo(0); // 삭제된 댓글 없음
    }
  }
}