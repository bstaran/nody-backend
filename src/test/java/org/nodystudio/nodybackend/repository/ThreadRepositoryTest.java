package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

  private User user1;
  private User user2;
  private Log log1;
  private Log log2;
  private Thread publicThread1;
  private Thread publicThread2;
  private Thread privateThread;
  private Thread independentThread;
  private Thread linkedThread;

  @BeforeEach
  void setUp() {
    LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
    setupTestData(baseTime);
  }

  /**
   * 스레드의 생성일시를 업데이트하는 공통 메서드
   */
  private void updateThreadCreatedAt(Long threadId, LocalDateTime createdAt) {
    entityManager.getEntityManager()
        .createQuery("UPDATE Thread t SET t.createdAt = :createdAt WHERE t.id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", threadId)
        .executeUpdate();
  }

  private void setupTestData(LocalDateTime baseTime) {
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

    log2 = Log.builder()
        .user(user2)
        .content("사용자2의 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .build();
    entityManager.persistAndFlush(log2);

    // 스레드 생성 (생성 시간을 의도적으로 다르게 설정)
    publicThread1 = Thread.builder()
        .user(user1)
        .log(log1)
        .content("공개 스레드 1 내용")
        .isPublic(true)
        .viewCount(10L)
        .build();
    entityManager.persistAndFlush(publicThread1);

    // 첫 번째 스레드가 가장 오래된 것
    updateThreadCreatedAt(publicThread1.getId(), baseTime);
    publicThread2 = Thread.builder()
        .user(user2)
        .log(log2)
        .content("공개 스레드 2 내용")
        .isPublic(true)
        .viewCount(5L)
        .build();
    entityManager.persistAndFlush(publicThread2);

    updateThreadCreatedAt(publicThread2.getId(), baseTime.plusMinutes(10));
    privateThread = Thread.builder()
        .user(user1)
        .log(log1)
        .content("비공개 스레드 내용")
        .isPublic(false)
        .viewCount(3L)
        .build();
    entityManager.persistAndFlush(privateThread);

    updateThreadCreatedAt(privateThread.getId(), baseTime.plusMinutes(5));
    independentThread = Thread.builder()
        .user(user1)
        .content("독립 스레드 내용")
        .isPublic(true)
        .viewCount(7L)
        .build();
    entityManager.persistAndFlush(independentThread);

    updateThreadCreatedAt(independentThread.getId(), baseTime.plusMinutes(20));
    linkedThread = Thread.builder()
        .user(user2)
        .log(log2)
        .content("로그 연결 스레드 내용")
        .isPublic(true)
        .viewCount(2L)
        .build();
    entityManager.persistAndFlush(linkedThread);

    // 마지막 스레드가 가장 최신
    updateThreadCreatedAt(linkedThread.getId(), baseTime.plusMinutes(30));

    entityManager.clear();
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
    Page<Thread> result = threadRepository.findPublicOrUserThreadsOrderByCreatedAtDesc(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(5); // 공개 스레드 4개 + user1의 비공개 스레드 1개
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "독립 스레드 내용", "공개 스레드 2 내용", "비공개 스레드 내용", "공개 스레드 1 내용");
    
    // user1이 볼 수 있는 스레드 확인 (공개 스레드 + 본인 스레드)
    assertThat(result.getContent())
        .allMatch(thread -> thread.getIsPublic() || thread.getUser().getId().equals(user1.getId()));
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("특정 로그의 공개 스레드 목록 조회 - 생성일 내림차순")
  void findPublicThreadsByLogIdOrderByCreatedAtDesc_WhenLogIdExists_ShouldReturnPublicThreadsForLog() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findPublicThreadsByLogIdOrderByCreatedAtDesc(log1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(1); // log1에 연결된 공개 스레드 1개
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("공개 스레드 1 내용");
  }

  @Test
  @DisplayName("특정 로그의 스레드 목록 조회 - 사용자 권한 고려")
  void findThreadsByLogIdWithUser_WhenUserAuthenticated_ShouldReturnPublicAndOwnThreadsForLog() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findThreadsByLogIdWithUser(log1.getId(), user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(2); // log1에 연결된 스레드 2개 (공개 1개 + user1의 비공개 1개)
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("공개 스레드 1 내용", "비공개 스레드 내용");
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("특정 사용자의 스레드 목록 조회 - 생성일 내림차순")
  void findByUserIdOrderByCreatedAtDesc_WhenUserIdExists_ShouldReturnUserThreadsSortedByCreatedAt() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findByUserIdOrderByCreatedAtDesc(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(3); // user1의 스레드 3개
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("독립 스레드 내용", "비공개 스레드 내용", "공개 스레드 1 내용");
    
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
    // user1의 독립 비공개 스레드 추가
    Thread privateIndependentThread = Thread.builder()
        .user(user1)
        .content("독립 비공개 스레드 내용")
        .isPublic(false)
        .build();
    entityManager.persistAndFlush(privateIndependentThread);

    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findIndependentThreadsWithUser(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(2); // 독립 스레드 2개 (공개 1개 + user1의 비공개 1개)
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    List<Thread> threads = result.getContent();
    assertThat(threads)
        .extracting(Thread::getContent)
        .containsExactly("독립 비공개 스레드 내용", "독립 스레드 내용");
    
    // 실제 생성일 순서 검증
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
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
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "공개 스레드 2 내용", "공개 스레드 1 내용");
    
    assertThat(result.getContent()).allMatch(thread -> thread.getLog() != null);
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("로그 연결 스레드 목록 조회 - 사용자 권한 고려")
  void findLinkedThreadsWithUser_WhenUserAuthenticated_ShouldReturnPublicAndOwnLinkedThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findLinkedThreadsWithUser(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(4); // 로그 연결 스레드 4개 (공개 3개 + user1의 비공개 1개)
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "공개 스레드 2 내용", "비공개 스레드 내용", "공개 스레드 1 내용");
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("페이징 기능 테스트 - 공개 스레드 목록 페이징")
  void findByIsPublicTrueOrderByCreatedAtDesc_WhenPaginationApplied_ShouldReturnCorrectPageInfo() {
    // given
    Pageable pageable = PageRequest.of(0, 2); // 페이지 크기 2

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