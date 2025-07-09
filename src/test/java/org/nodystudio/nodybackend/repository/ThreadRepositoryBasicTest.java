package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
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

  private ThreadRepositoryTestDataHelper.TestDataContainer testData;

  @BeforeEach
  void setUp() {
    ThreadRepositoryTestDataHelper.TestDataBuilder builder = new ThreadRepositoryTestDataHelper.TestDataBuilder(
        entityManager)
        .withCustomUser(u -> u
            .email("user1@example.com")
            .nickname("user1")
            .socialId("123456"))
        .withCustomUser(u -> u
            .email("user2@example.com")
            .nickname("user2")
            .socialId("234567"))
        .buildUsersAndLogs();

    // 기본 로그 연결 테스트용
    builder.withCustomLog(l -> l
            .content("사용자1의 로그")
            .user(builder.getCurrentData().users.get(0)))
        .buildUsersAndLogs();

    Log log1 = builder.getCurrentData().logs.get(0);

    // 로그와 연결된 스레드들 생성
    testData = builder
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("공개 스레드 1 내용")
            .isPublic(true)
            .viewCount(10L)
            .log(log1))
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("비공개 스레드 내용")
            .isPublic(false)
            .viewCount(3L)
            .log(log1))
        .finalizeWithTimestamps();
  }

  @Test
  @DisplayName("특정 사용자의 스레드 ID로 조회 - 성공")
  void findByIdAndUserId_WhenValidUserIdAndThreadId_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndUserId(testData.threads.get(0).getId(),
        testData.users.get(0).getId());

    // then
    assertThat(result).isPresent();
    Thread thread = result.get();
    assertThat(thread.getContent()).isEqualTo("공개 스레드 1 내용");
    assertThat(thread.getUser().getId()).isEqualTo(testData.users.get(0).getId());
    assertThat(thread.getIsPublic()).isTrue();
    assertThat(thread.getLog()).isNotNull();
    assertThat(thread.getLog().getId()).isEqualTo(testData.logs.get(0).getId());
  }

  @Test
  @DisplayName("다른 사용자의 스레드 조회 시 빈 결과 반환")
  void findByIdAndUserId_WhenDifferentUserId_ShouldReturnEmpty() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndUserId(testData.threads.get(0).getId(),
        testData.users.get(1).getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("공개 스레드 ID로 조회 - 성공")
  void findByIdAndIsPublicTrue_WhenPublicThread_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndIsPublicTrue(
        testData.threads.get(0).getId());

    // then
    assertThat(result).isPresent();
    Thread thread = result.get();
    assertThat(thread.getContent()).isEqualTo("공개 스레드 1 내용");
    assertThat(thread.getIsPublic()).isTrue();
    assertThat(thread.getUser().getId()).isEqualTo(testData.users.get(0).getId());
    assertThat(thread.getViewCount()).isEqualTo(10L);
  }

  @Test
  @DisplayName("비공개 스레드를 공개 조회로 찾을 수 없음")
  void findByIdAndIsPublicTrue_WhenPrivateThread_ShouldReturnEmpty() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndIsPublicTrue(
        testData.threads.get(1).getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 조회 - 공개 스레드")
  void findViewableThreadByIdAndUserId_WhenPublicThread_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        testData.threads.get(0).getId(), testData.users.get(1).getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("공개 스레드 1 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 조회 - 본인의 비공개 스레드")
  void findViewableThreadByIdAndUserId_WhenOwnPrivateThread_ShouldReturnThread() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        testData.threads.get(1).getId(), testData.users.get(0).getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("비공개 스레드 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 없는 스레드 조회 - 다른 사용자의 비공개 스레드")
  void findViewableThreadByIdAndUserId_WhenOtherUserPrivateThread_ShouldReturnEmpty() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        testData.threads.get(1).getId(), testData.users.get(1).getId());

    // then
    assertThat(result).isEmpty();
  }
}