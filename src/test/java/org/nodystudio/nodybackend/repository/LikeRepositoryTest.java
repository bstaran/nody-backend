package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.domain.like.Like;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.jpa.defer-datasource-initialization=false"
})
@DisplayName("LikeRepository 테스트")
class LikeRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private LikeRepository likeRepository;

  private User testUser;
  private Thread testThread;
  private Log testLog;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = User.builder()
        .provider(OAuthProvider.GOOGLE)
        .socialId("12345")
        .email("test@example.com")
        .nickname("testuser")
        .build();
    entityManager.persistAndFlush(testUser);

    // 테스트 스레드 생성
    testThread = Thread.builder()
        .user(testUser)
        .content("테스트 스레드 내용")
        .build();
    entityManager.persistAndFlush(testThread);

    // 테스트 로그 생성
    testLog = Log.builder()
        .user(testUser)
        .content("테스트 로그 내용")
        .build();
    entityManager.persistAndFlush(testLog);
  }

  private Like createLike(User user, TargetType targetType, Long targetId) {
    return Like.builder()
        .user(user)
        .targetType(targetType)
        .targetId(targetId)
        .isActive(true)
        .build();
  }

  private Like createLike(User user, TargetType targetType, Long targetId, boolean isActive) {
    return Like.builder()
        .user(user)
        .targetType(targetType)
        .targetId(targetId)
        .isActive(isActive)
        .build();
  }

  private User createUser(String email, String nickname) {
    User user = User.builder()
        .provider(OAuthProvider.GOOGLE)
        .socialId(email)
        .email(email)
        .nickname(nickname)
        .build();
    return entityManager.persistAndFlush(user);
  }

  @Nested
  @DisplayName("좋아요 조회 테스트")
  class FindLikeTest {

    @Test
    @DisplayName("사용자가 스레드에 좋아요를 눌렀을 때 조회된다")
    void findByUserIdAndTargetTypeAndTargetId_WithThreadLike_ShouldReturnLike() {
      // Given
      Like like = createLike(testUser, TargetType.THREAD, testThread.getId());
      entityManager.persistAndFlush(like);

      // When
      Optional<Like> result = likeRepository.findByUserIdAndTargetTypeAndTargetId(
          testUser.getId(), TargetType.THREAD, testThread.getId());

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
      assertThat(result.get().getTargetType()).isEqualTo(TargetType.THREAD);
      assertThat(result.get().getTargetId()).isEqualTo(testThread.getId());
    }

    @Test
    @DisplayName("사용자가 로그에 좋아요를 눌렀을 때 조회된다")
    void findByUserIdAndTargetTypeAndTargetId_WithLogLike_ShouldReturnLike() {
      // Given
      Like like = createLike(testUser, TargetType.LOG, testLog.getId());
      entityManager.persistAndFlush(like);

      // When
      Optional<Like> result = likeRepository.findByUserIdAndTargetTypeAndTargetId(
          testUser.getId(), TargetType.LOG, testLog.getId());

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
      assertThat(result.get().getTargetType()).isEqualTo(TargetType.LOG);
      assertThat(result.get().getTargetId()).isEqualTo(testLog.getId());
    }

    @Test
    @DisplayName("좋아요가 없으면 empty를 반환한다")
    void findByUserIdAndTargetTypeAndTargetId_WithoutLike_ShouldReturnEmpty() {
      // When
      Optional<Like> result = likeRepository.findByUserIdAndTargetTypeAndTargetId(
          testUser.getId(), TargetType.THREAD, testThread.getId());

      // Then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("좋아요 개수 조회 테스트")
  class CountLikeTest {

    @Test
    @DisplayName("스레드의 활성 좋아요 개수를 정확히 조회한다")
    void countByTargetTypeAndTargetIdAndIsActiveTrue_WithThreadLikes_ShouldReturnCorrectCount() {
      // Given
      User user1 = createUser("user1@example.com", "user1");
      User user2 = createUser("user2@example.com", "user2");

      Like like1 = createLike(user1, TargetType.THREAD, testThread.getId());
      Like like2 = createLike(user2, TargetType.THREAD, testThread.getId());

      entityManager.persistAndFlush(like1);
      entityManager.persistAndFlush(like2);

      // When
      long count = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD,
          testThread.getId());

      // Then
      assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("로그의 활성 좋아요 개수를 정확히 조회한다")
    void countByTargetTypeAndTargetIdAndIsActiveTrue_WithLogLikes_ShouldReturnCorrectCount() {
      // Given
      User user1 = createUser("user1@example.com", "user1");
      Like like = createLike(user1, TargetType.LOG, testLog.getId());
      entityManager.persistAndFlush(like);

      // When
      long count = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.LOG, testLog.getId());

      // Then
      assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("활성 좋아요가 없으면 0을 반환한다")
    void countByTargetTypeAndTargetIdAndIsActiveTrue_WithoutLikes_ShouldReturnZero() {
      // When
      long count = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD,
          testThread.getId());

      // Then
      assertThat(count).isEqualTo(0L);
    }

    @Test
    @DisplayName("비활성 좋아요는 카운트되지 않는다")
    void countByTargetTypeAndTargetIdAndIsActiveTrue_WithInactiveLikes_ShouldReturnZero() {
      // Given
      User user1 = createUser("user1@example.com", "user1");
      Like inactiveLike = createLike(user1, TargetType.THREAD, testThread.getId(), false);
      entityManager.persistAndFlush(inactiveLike);

      // When
      long count = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD,
          testThread.getId());

      // Then
      assertThat(count).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("좋아요 존재 확인 테스트")
  class ExistsLikeTest {

    @Test
    @DisplayName("사용자가 스레드에 좋아요를 눌렀으면 true를 반��한다")
    void existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue_WithActiveLike_ShouldReturnTrue() {
      // Given
      Like like = createLike(testUser, TargetType.THREAD, testThread.getId());
      entityManager.persistAndFlush(like);

      // When
      boolean exists = likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(
          testUser.getId(), TargetType.THREAD, testThread.getId());

      // Then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자가 좋아요를 누르지 않았으면 false를 반환한다")
    void existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue_WithoutLike_ShouldReturnFalse() {
      // When
      boolean exists = likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(
          testUser.getId(), TargetType.THREAD, testThread.getId());

      // Then
      assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("비활성 좋아요는 존재하지 않는 것으로 처리된다")
    void existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue_WithInactiveLike_ShouldReturnFalse() {
      // Given
      Like inactiveLike = createLike(testUser, TargetType.THREAD, testThread.getId(), false);
      entityManager.persistAndFlush(inactiveLike);

      // When
      boolean exists = likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(
          testUser.getId(), TargetType.THREAD, testThread.getId());

      // Then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("좋아요 삭제 테스트")
  class DeleteLikeTest {

    @Test
    @DisplayName("사용자의 좋아요를 성공적으로 삭제한다")
    void deleteByUserIdAndTargetTypeAndTargetId_WithLike_ShouldDeleteLike() {
      // Given
      Like like = createLike(testUser, TargetType.THREAD, testThread.getId());
      entityManager.persistAndFlush(like);

      // When
      likeRepository.deleteByUserIdAndTargetTypeAndTargetId(
          testUser.getId(), TargetType.THREAD, testThread.getId());
      entityManager.flush();

      // Then
      boolean exists = likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(
          testUser.getId(), TargetType.THREAD, testThread.getId());
      assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("좋아요가 없어도 예외가 발생하지 않는다")
    void deleteByUserIdAndTargetTypeAndTargetId_WithoutLike_ShouldNotThrowException() {
      // When & Then
      likeRepository.deleteByUserIdAndTargetTypeAndTargetId(
          testUser.getId(), TargetType.THREAD, testThread.getId());
      entityManager.flush();
    }
  }
}
