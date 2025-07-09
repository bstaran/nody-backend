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
@DisplayName("ThreadRepository 테스트")
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
  @DisplayName("사용자별 스레드 조회")
  void findByIdAndUserId_Success() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndUserId(publicThread1.getId(), user1.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("공개 스레드 1 내용");
    assertThat(result.get().getUser().getId()).isEqualTo(user1.getId());
  }

  @Test
  @DisplayName("다른 사용자의 스레드 조회 시 빈 결과")
  void findByIdAndUserId_OtherUserThread_ReturnsEmpty() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndUserId(publicThread1.getId(), user2.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("공개 스레드 조회")
  void findByIdAndIsPublicTrue_Success() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndIsPublicTrue(publicThread1.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("공개 스레드 1 내용");
    assertThat(result.get().getIsPublic()).isTrue();
  }

  @Test
  @DisplayName("비공개 스레드를 공개 조회로 찾을 수 없음")
  void findByIdAndIsPublicTrue_PrivateThread_ReturnsEmpty() {
    // when
    Optional<Thread> result = threadRepository.findByIdAndIsPublicTrue(privateThread.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 조회 - 공개 스레드")
  void findViewableThreadByIdAndUserId_PublicThread_Success() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        publicThread2.getId(), user1.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("공개 스레드 2 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 조회 - 본인의 비공개 스레드")
  void findViewableThreadByIdAndUserId_OwnPrivateThread_Success() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        privateThread.getId(), user1.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("비공개 스레드 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 없는 스레드 조회 - 다른 사용자의 비공개 스레드")
  void findViewableThreadByIdAndUserId_OtherUserPrivateThread_ReturnsEmpty() {
    // when
    Optional<Thread> result = threadRepository.findViewableThreadByIdAndUserId(
        privateThread.getId(), user2.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("공개 스레드 목록 조회")
  void findByIsPublicTrueOrderByCreatedAtDesc_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findByIsPublicTrueOrderByCreatedAtDesc(pageable);

    // then
    assertThat(result.getContent()).hasSize(4); // 공개 스레드 4개
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "독립 스레드 내용", "공개 스레드 2 내용", "공개 스레드 1 내용");
  }

  @Test
  @DisplayName("사용자가 볼 수 있는 스레드 목록 조회")
  void findPublicOrUserThreadsOrderByCreatedAtDesc_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findPublicOrUserThreadsOrderByCreatedAtDesc(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(5); // 공개 스레드 4개 + user1의 비공개 스레드 1개
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .contains("공개 스레드 1 내용", "공개 스레드 2 내용", "비공개 스레드 내용", "독립 스레드 내용", "로그 연결 스레드 내용");
  }

  @Test
  @DisplayName("특정 로그의 공개 스레드 목록 조회")
  void findPublicThreadsByLogIdOrderByCreatedAtDesc_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findPublicThreadsByLogIdOrderByCreatedAtDesc(log1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(1); // log1에 연결된 공개 스레드 1개
    assertThat(result.getContent().getFirst().getContent()).isEqualTo("공개 스레드 1 내용");
  }

  @Test
  @DisplayName("특정 로그의 스레드 목록 조회 (사용자별 권한 고려)")
  void findThreadsByLogIdWithUser_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findThreadsByLogIdWithUser(log1.getId(), user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(2); // log1에 연결된 스레드 2개 (공개 1개 + user1의 비공개 1개)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("공개 스레드 1 내용", "비공개 스레드 내용");
  }

  @Test
  @DisplayName("특정 사용자의 스레드 목록 조회")
  void findByUserIdOrderByCreatedAtDesc_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findByUserIdOrderByCreatedAtDesc(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(3); // user1의 스레드 3개
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("공개 스레드 1 내용", "비공개 스레드 내용", "독립 스레드 내용");
  }

  @Test
  @DisplayName("독립 공개 스레드 목록 조회")
  void findIndependentPublicThreadsOrderByCreatedAtDesc_Success() {
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
  @DisplayName("독립 스레드 목록 조회 (사용자별 권한 고려)")
  void findIndependentThreadsWithUser_Success() {
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
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("독립 스레드 내용", "독립 비공개 스레드 내용");
  }

  @Test
  @DisplayName("로그 연결 공개 스레드 목록 조회")
  void findLinkedPublicThreadsOrderByCreatedAtDesc_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findLinkedPublicThreadsOrderByCreatedAtDesc(pageable);

    // then
    assertThat(result.getContent()).hasSize(3); // 로그 연결 공개 스레드 3개
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("공개 스레드 1 내용", "공개 스레드 2 내용", "로그 연결 스레드 내용");
    assertThat(result.getContent()).allMatch(thread -> thread.getLog() != null);
  }

  @Test
  @DisplayName("로그 연결 스레드 목록 조회 (사용자별 권한 고려)")
  void findLinkedThreadsWithUser_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.findLinkedThreadsWithUser(user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(4); // 로그 연결 스레드 4개 (공개 3개 + user1의 비공개 1개)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("공개 스레드 1 내용", "공개 스레드 2 내용", "로그 연결 스레드 내용", "비공개 스레드 내용");
  }

  @Test
  @DisplayName("내용으로 공개 스레드 검색")
  void searchPublicThreadsByContent_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.searchPublicThreadsByContent("공개", pageable);

    // then
    assertThat(result.getContent()).hasSize(2); // "공개"가 포함된 공개 스레드 2개
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("공개 스레드 1 내용", "공개 스레드 2 내용");
  }

  @Test
  @DisplayName("내용으로 스레드 검색 (사용자별 권한 고려)")
  void searchThreadsByContentWithUser_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.searchThreadsByContentWithUser("스레드", user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(5); // "스레드"가 포함된 스레드 5개 (user1이 볼 수 있는 모든 스레드)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("공개 스레드 1 내용", "공개 스레드 2 내용", "비공개 스레드 내용", "독립 스레드 내용", "로그 연결 스레드 내용");
  }

  @Test
  @DisplayName("대소문자 구분 없는 검색")
  void searchPublicThreadsByContent_CaseInsensitive_Success() {
    // given
    // 영문 대소문자 혼용 스레드 추가
    Thread mixedCaseThread1 = Thread.builder()
        .user(user1)
        .content("Public Content Thread")
        .isPublic(true)
        .build();
    entityManager.persistAndFlush(mixedCaseThread1);
    
    Thread mixedCaseThread2 = Thread.builder()
        .user(user2)
        .content("public content thread")
        .isPublic(true)
        .build();
    entityManager.persistAndFlush(mixedCaseThread2);
    
    Pageable pageable = PageRequest.of(0, 10);

    // when - 소문자로 검색
    Page<Thread> result = threadRepository.searchPublicThreadsByContent("public", pageable);

    // then - 대소문자 관계없이 모두 검색되어야 함
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactlyInAnyOrder("Public Content Thread", "public content thread");
  }

  @Test
  @DisplayName("특정 로그의 스레드 개수 조회")
  void countByLogId_Success() {
    // when
    long count = threadRepository.countByLogId(log1.getId());

    // then
    assertThat(count).isEqualTo(2); // log1에 연결된 스레드 2개 (공개 1개 + 비공개 1개)
  }

  @Test
  @DisplayName("특정 사용자의 스레드 개수 조회")
  void countByUserId_Success() {
    // when
    long count = threadRepository.countByUserId(user1.getId());

    // then
    assertThat(count).isEqualTo(3); // user1이 작성한 스레드 3개
  }

  @Test
  @DisplayName("존재하지 않는 로그의 스레드 개수 조회")
  void countByLogId_NonExistentLog_ReturnsZero() {
    // when
    long count = threadRepository.countByLogId(Long.MAX_VALUE);

    // then
    assertThat(count).isZero();
  }

  @Test
  @DisplayName("페이징 기능 테스트")
  void findByIsPublicTrueOrderByCreatedAtDesc_Pagination_Success() {
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
  @DisplayName("정렬 기능 테스트 - 생성일 기준 내림차순")
  void findByIsPublicTrueOrderByCreatedAtDesc_Sorting_Success() {
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