package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.RoleType;
import org.nodystudio.nodybackend.domain.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ThreadRepository 기본 조회 테스트")
class ThreadRepositoryBasicTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ThreadRepository threadRepository;

  private User user1;
  private User user2;
  private Log log1;
  private Thread publicThread1;
  private Thread privateThread;

  @BeforeEach
  void setUp() {
    setupTestData();
  }

  private void setupTestData() {
    // 사용자 생성
    user1 = User.builder()
        .email("user1@example.com")
        .nickname("user1")
        .provider("google")
        .socialId("123456")
        .role(RoleType.USER)
        .build();
    entityManager.persistAndFlush(user1);

    user2 = User.builder()
        .email("user2@example.com")
        .nickname("user2")
        .provider("google")
        .socialId("234567")
        .role(RoleType.USER)
        .build();
    entityManager.persistAndFlush(user2);

    // 로그 생성
    log1 = Log.builder()
        .user(user1)
        .content("사용자1의 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .build();
    entityManager.persistAndFlush(log1);

    // 스레드 생성
    publicThread1 = Thread.builder()
        .user(user1)
        .log(log1)
        .content("공개 스레드 1 내용")
        .isPublic(true)
        .viewCount(10L)
        .build();
    entityManager.persistAndFlush(publicThread1);

    privateThread = Thread.builder()
        .user(user1)
        .log(log1)
        .content("비공개 스레드 내용")
        .isPublic(false)
        .viewCount(3L)
        .build();
    entityManager.persistAndFlush(privateThread);

    entityManager.clear();
  }

  @Test
  @DisplayName("특정 사용자의 스레드 ID로 조회 - 성공")
  void findByIdAndUserId_WhenValidUserIdAndThreadId_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndUserId(publicThread1.getId(), user1.getId());

    // then
    assertThat(result).isPresent();
    Thread thread = result.get();
    assertThat(thread.getContent()).isEqualTo("공개 스레드 1 내용");
    assertThat(thread.getUser().getId()).isEqualTo(user1.getId());
    assertThat(thread.getIsPublic()).isTrue();
    assertThat(thread.getLog()).isNotNull();
    assertThat(thread.getLog().getId()).isEqualTo(log1.getId());
  }

  @Test
  @DisplayName("다른 사용자의 스레드 조회 시 빈 결과 반환")
  void findByIdAndUserId_WhenDifferentUserId_ShouldReturnEmpty() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndUserId(publicThread1.getId(), user2.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("공개 스레드 ID로 조회 - 성공")
  void findByIdAndIsPublicTrue_WhenPublicThread_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndIsPublicTrue(publicThread1.getId());

    // then
    assertThat(result).isPresent();
    Thread thread = result.get();
    assertThat(thread.getContent()).isEqualTo("공개 스레드 1 내용");
    assertThat(thread.getIsPublic()).isTrue();
    assertThat(thread.getUser().getId()).isEqualTo(user1.getId());
    assertThat(thread.getViewCount()).isEqualTo(10L);
  }

  @Test
  @DisplayName("비공개 스레드를 공개 조회로 찾을 수 없음")
  void findByIdAndIsPublicTrue_WhenPrivateThread_ShouldReturnEmpty() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndIsPublicTrue(privateThread.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 조회 - 공개 스레드")
  void findViewableThreadByIdAndUserId_WhenPublicThread_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        publicThread1.getId(), user2.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("공개 스레드 1 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 조회 - 본인의 비공개 스레드")
  void findViewableThreadByIdAndUserId_WhenOwnPrivateThread_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        privateThread.getId(), user1.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("비공개 스레드 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 없는 스레드 조회 - 다른 사용자의 비공개 스레드")
  void findViewableThreadByIdAndUserId_WhenOtherUserPrivateThread_ShouldReturnEmpty() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        privateThread.getId(), user2.getId());

    // then
    assertThat(result).isEmpty();
  }
}