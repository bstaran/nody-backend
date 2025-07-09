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
@DisplayName("ThreadRepository 목록 조회 및 페이징 테스트")
class ThreadRepositoryTest {

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
        .withTimestamps(baseTime, 5)
        .withCustomUser(u -> u
            .email("user1@example.com")
            .nickname("user1")
            .socialId("123456"))
        .withCustomUser(u -> u
            .email("user2@example.com")
            .nickname("user2")
            .socialId("234567"))
        .buildUsersAndLogs();

    builder.withCustomLog(l -> l
            .content("사용자1의 로그")
            .user(builder.getCurrentData().users.get(0)))
        .withCustomLog(l -> l
            .content("사용자2의 로그")
            .user(builder.getCurrentData().users.get(1)))
        .buildUsersAndLogs();

    Log log1 = builder.getCurrentData().logs.get(0);

    testData = builder
        // log1에 연결된 스레드들
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
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("공개 스레드 2 내용")
            .isPublic(true)
            .viewCount(5L)
            .log(log1))
        // 독립 스레드들 (로그 없음)
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("독립 스레드 내용")
            .isPublic(true)
            .viewCount(7L))
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("독립 비공개 스레드 내용")
            .isPublic(false)
            .viewCount(0L))
        // 다시 log1에 연결된 스레드
        .addThread(t -> t
            .user(builder.getCurrentData().users.get(0))
            .content("로그 연결 스레드 내용")
            .isPublic(true)
            .viewCount(2L)
            .log(log1))
        .finalizeWithTimestamps();
  }

  @Test
  @DisplayName("공개 스레드 목록 조회 - 생성일 내림차순 정렬")
  void findByIsPublicTrueOrderByCreatedAtDesc_WhenPublicThreadsExist_ShouldReturnSortedList() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);

    // then
    assertThat(result.getContent()).hasSize(4); // 공개 스레드 4개
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "독립 스레드 내용", "공개 스레드 2 내용", "공개 스레드 1 내용");
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 목록 조회 - 공개+본인 스레드")
  void findPublicOrUserThreadsOrderByCreatedAtDesc_WhenUserAuthenticated_ShouldReturnPublicAndOwnThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findPublicOrUserThreadsOrderByCreatedAtDesc(
        testData.users.get(0).getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(6);

    // user1이 볼 수 있는 스레드 확인 (공개 스레드 + 본인 스레드)
    assertThat(result.getContent())
        .allMatch(thread -> thread.getIsPublic() || thread.getUser().getId()
            .equals(testData.users.get(0).getId()));
  }

  @Test
  @DisplayName("특정 로그의 공개 스레드 목록 조회 - 생성일 내림차순")
  void findPublicThreadsByLogIdOrderByCreatedAtDesc_WhenLogIdExists_ShouldReturnPublicThreadsForLog() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findPublicThreadsByLogIdOrderByCreatedAtDesc(
        testData.logs.get(0).getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(3); // log1에 연결된 공개 스레드 3개

    // 생성일 내림차순으로 가장 최신 스레드 검증
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("로그 연결 스레드 내용");
  }

  @Test
  @DisplayName("특정 로그의 스레드 목록 조회 - 사용자 권한 고려")
  void findThreadsByLogIdWithUser_WhenUserAuthenticated_ShouldReturnPublicAndOwnThreadsForLog() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findThreadsByLogIdWithUser(testData.logs.get(0).getId(),
        testData.users.get(0).getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(4); // log1에 연결된 스레드 4개 (공개 3개 + user1의 비공개 1개)
  }

  @Test
  @DisplayName("특정 사용자의 스레드 목록 조회 - 생성일 내림차순")
  void findByUserIdOrderByCreatedAtDesc_WhenUserIdExists_ShouldReturnUserThreadsSortedByCreatedAt() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findByUserIdOrderByCreatedAtDesc(
        testData.users.get(0).getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(6); // user1의 스레드 6개 (모든 스레드가 user1 소유)
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "독립 비공개 스레드 내용", "독립 스레드 내용", "공개 스레드 2 내용", "비공개 스레드 내용",
            "공개 스레드 1 내용");
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("독립 공개 스레드 목록 조회 - 로그에 연결되지 않은 스레드")
  void findIndependentPublicThreadsOrderByCreatedAtDesc_WhenIndependentThreadsExist_ShouldReturnPublicThreadsNotLinkedToLog() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findIndependentPublicThreadsOrderByCreatedAtDesc(pageable);

    // then
    assertThat(result.getContent()).hasSize(1); // 독립 공개 스레드 1개
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("독립 스레드 내용");
    assertThat(result.getContent().getFirst().getLog()).isNull();
  }

  @Test
  @DisplayName("독립 스레드 목록 조회 - 사용자 권한 고려")
  void findIndependentThreadsWithUser_WhenUserAuthenticated_ShouldReturnPublicAndOwnIndependentThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findIndependentThreadsWithUser(
        testData.users.get(0).getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(2); // 독립 스레드 2개 (공개 1개 + user1의 비공개 1개)
  }

  @Test
  @DisplayName("로그 연결 공개 스레드 목록 조회 - 로그에 연결된 스레드")
  void findLinkedPublicThreadsOrderByCreatedAtDesc_WhenLinkedThreadsExist_ShouldReturnPublicThreadsLinkedToLog() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findLinkedPublicThreadsOrderByCreatedAtDesc(pageable);

    // then
    assertThat(result.getContent()).hasSize(3); // 로그 연결 공개 스레드 3개
    assertThat(result.getContent()).allMatch(thread -> thread.getLog() != null);
  }

  @Test
  @DisplayName("로그 연결 스레드 목록 조회 - 사용자 권한 고려")
  void findLinkedThreadsWithUser_WhenUserAuthenticated_ShouldReturnPublicAndOwnLinkedThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findLinkedThreadsWithUser(testData.users.get(0).getId(),
        pageable);

    // then
    assertThat(result.getContent()).hasSize(4); // 로그 연결 스레드 4개 (공개 3개 + user1의 비공개 1개)
  }

  @Test
  @DisplayName("페이징 기능 테스트 - 공개 스레드 목록 페이징")
  void findByIsPublicTrueOrderByCreatedAtDesc_WhenPaginationApplied_ShouldReturnCorrectPageInfo() {
    // given
    Pageable pageable = PageRequest.of(0, 2);

    // when
    Page<Thread> result = threadRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(4);
    assertThat(result.getTotalPages()).isEqualTo(2);
    assertThat(result.isFirst()).isTrue();
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  @DisplayName("정렬 기능 테스트 - 생성일 기준 내림차순 정렬 검증")
  void findByIsPublicTrueOrderByCreatedAtDesc_WhenSortingApplied_ShouldReturnThreadsInDescendingOrder() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);

    // then
    List<Thread> content = result.getContent();
    for (int i = 0; i < content.size() - 1; i++) {
      assertThat(content.get(i).getCreatedAt())
          .isAfterOrEqualTo(content.get(i + 1).getCreatedAt());
    }
  }
}