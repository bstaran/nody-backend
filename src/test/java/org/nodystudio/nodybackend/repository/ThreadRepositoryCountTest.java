package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ThreadRepository 집계 테스트")
class ThreadRepositoryCountTest {

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

    // log별 카운트 테스트용 로그 - 사용자 명시적 지정
    builder.withCustomLog(l -> l
            .content("사용자1의 로그")
            .user(builder.getCurrentData().users.get(0)))
        .buildUsersAndLogs();

    Log log1 = builder.getCurrentData().logs.get(0);

    // 스레드들 생성
    testData = builder
        // user1의 로그 연결 스레드들
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
        // user2의 독립 스레드
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(1))
            .content("다른 사용자 스레드")
            .isPublic(true)
            .viewCount(1L))
        .finalizeWithTimestamps();
  }

  @Test
  @DisplayName("특정 로그의 스레드 개수 조회 - 전체 스레드 개수")
  void countByLogId_WhenLogIdExists_ShouldReturnTotalThreadCount() {
    // when
    long count = threadRepository.countByLogIdAndDeactivatedAtIsNull(testData.logs.get(0).getId());

    // then
    assertThat(count).isEqualTo(2); // log1에 연결된 스레드 2개 (공개 1개 + 비공개 1개)
  }

  @Test
  @DisplayName("특정 사용자의 스레드 개수 조회 - 사용자 작성 스레드 개수")
  void countByUserId_WhenUserIdExists_ShouldReturnUserThreadCount() {
    // when
    long count = threadRepository.countByUserIdAndDeactivatedAtIsNull(testData.users.get(0).getId());

    // then
    assertThat(count).isEqualTo(2); // user1이 작성한 스레드 2개
  }

  @Test
  @DisplayName("존재하지 않는 로그의 스레드 개수 조회 - 0 반환")
  void countByLogId_WhenLogIdDoesNotExist_ShouldReturnZero() {
    // when
    long count = threadRepository.countByLogIdAndDeactivatedAtIsNull(Long.MAX_VALUE);

    // then
    assertThat(count).isZero();
  }

  @Test
  @DisplayName("존재하지 않는 사용자의 스레드 개수 조회 - 0 반환")
  void countByUserId_WhenUserIdDoesNotExist_ShouldReturnZero() {
    // when
    long count = threadRepository.countByUserIdAndDeactivatedAtIsNull(Long.MAX_VALUE);

    // then
    assertThat(count).isZero();
  }
}