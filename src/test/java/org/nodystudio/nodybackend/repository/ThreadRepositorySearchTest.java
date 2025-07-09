package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ThreadRepository 검색 테스트")
class ThreadRepositorySearchTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ThreadRepository threadRepository;

  private ThreadRepositoryTestDataHelper.TestDataContainer testData;

  @BeforeEach
  void setUp() {
    LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);

    ThreadRepositoryTestDataHelper.TestDataBuilder builder = new ThreadRepositoryTestDataHelper.TestDataBuilder(
        entityManager)
        .withTimestamps(baseTime, 10)
        .withCustomUser(u -> u
            .email("user1@example.com")
            .nickname("user1")
            .socialId("123456"))
        .withCustomUser(u -> u
            .email("user2@example.com")
            .nickname("user2")
            .socialId("234567"))
        .buildUsersAndLogs();

    // 검색용 로그 - 사용자 명시적 지정
    builder.withCustomLog(l -> l
            .content("사용자1의 로그")
            .user(builder.getCurrentData().users.get(0)))
        .buildUsersAndLogs();

    Log log1 = builder.getCurrentData().logs.get(0);
    for (String content : List.of("공개 스레드 1 내용", "비공개 스레드 내용", "공개 스레드 2 내용")) {
      builder.addThread(t -> t
          .user(builder.getCurrentData().users.get(0))
          .content(content)
          .isPublic(!content.contains("비공개"))
          .viewCount(content.contains("1") ? 10L : content.contains("비공개") ? 3L : 5L)
          .log(log1));
    }

    // 독립 스레드와 로그 연결 스레드
    builder
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("독립 스레드 내용")
            .isPublic(true)
            .viewCount(7L))
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("로그 연결 스레드 내용")
            .isPublic(true)
            .viewCount(2L)
            .log(log1));

    // 대소문자 테스트용 스레드들
    for (String content : List.of("Public Content Thread", "public content thread")) {
      builder.addThread(t -> t
          .user(builder.getCurrentData().users.get(0))
          .content(content)
          .isPublic(true)
          .viewCount(0L));
    }

    testData = builder.finalizeWithTimestamps();
  }

  @Test
  @DisplayName("내용으로 공개 스레드 검색 - 특정 키워드 포함 스레드 ")
  void searchPublicThreadsByContent_WhenContentContainsKeyword_ShouldReturnMatchingPublicThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.searchPublicThreadsByContent("공개", pageable);

    // then
    assertThat(result.getContent()).hasSize(2); // "공개"가 포함된 공개 스레드 2개
  }

  @Test
  @DisplayName("내용으로 스레드 검색 - 사용자 권한 고려")
  void searchThreadsByContentWithUser_WhenUserAuthenticated_ShouldReturnMatchingPublicAndOwnThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.searchThreadsByContentWithUser("스레드",
        testData.users.get(0).getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(5); // "스레드"가 포함된 스레드 5개 (user1이 볼 수 있는 모든 스레드)
  }

  @Test
  @DisplayName("대소문자 구분 없는 검색 - 영문 대소문자 혼용")
  void searchPublicThreadsByContent_WhenMixedCaseKeyword_ShouldReturnMatchingThreadsIgnoringCase() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when - 소문자로 검색
    Page<Thread> result = threadRepository.searchPublicThreadsByContent("public", pageable);

    // then - 대소문자 관계없이 모두 검색되어야 함
    assertThat(result.getContent()).hasSize(2);
  }
}