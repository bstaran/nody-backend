package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
@DisplayName("ThreadRepository 검색 테스트")
class ThreadRepositorySearchTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ThreadRepository threadRepository;

  private User user1;
  private User user2;
  private Log log1;
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

    Log log2 = Log.builder()
        .user(user2)
        .content("사용자2의 로그")
        .latitude(new BigDecimal("37.5665"))
        .longitude(new BigDecimal("126.9780"))
        .build();
    entityManager.persistAndFlush(log2);

    // 검색용 스레드 생성
    publicThread1 = Thread.builder()
        .user(user1)
        .log(log1)
        .content("공개 스레드 1 내용")
        .isPublic(true)
        .viewCount(10L)
        .build();
    entityManager.persistAndFlush(publicThread1);
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
    updateThreadCreatedAt(linkedThread.getId(), baseTime.plusMinutes(30));

    entityManager.clear();
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
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("공개 스레드 2 내용", "공개 스레드 1 내용");
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("내용으로 스레드 검색 - 사용자 권한 고려")
  void searchThreadsByContentWithUser_WhenUserAuthenticated_ShouldReturnMatchingPublicAndOwnThreads() {
    // given
    Pageable pageable = PageRequest.of(0, 10);

    // when
    Page<Thread> result = threadRepository.searchThreadsByContentWithUser("스레드", user1.getId(), pageable);

    // then
    assertThat(result.getContent()).hasSize(5); // "스레드"가 포함된 스레드 5개 (user1이 볼 수 있는 모든 스레드)
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    assertThat(result.getContent())
        .extracting(Thread::getContent)
        .containsExactly("로그 연결 스레드 내용", "독립 스레드 내용", "공개 스레드 2 내용", "비공개 스레드 내용", "공개 스레드 1 내용");
    
    // 실제 생성일 순서 검증
    List<Thread> threads = result.getContent();
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }

  @Test
  @DisplayName("대소문자 구분 없는 검색 - 영문 대소문자 혼용")
  void searchPublicThreadsByContent_WhenMixedCaseKeyword_ShouldReturnMatchingThreadsIgnoringCase() {
    // given
    LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
    
    // 영문 대소문자 혼용 스레드 추가
    Thread mixedCaseThread1 = Thread.builder()
        .user(user1)
        .content("Public Content Thread")
        .isPublic(true)
        .build();
    entityManager.persistAndFlush(mixedCaseThread1);
    updateThreadCreatedAt(mixedCaseThread1.getId(), baseTime.plusMinutes(40));
    
    Thread mixedCaseThread2 = Thread.builder()
        .user(user2)
        .content("public content thread")
        .isPublic(true)
        .build();
    entityManager.persistAndFlush(mixedCaseThread2);
    updateThreadCreatedAt(mixedCaseThread2.getId(), baseTime.plusMinutes(50));
    
    entityManager.clear(); // 엔티티 캐시 클리어
    
    Pageable pageable = PageRequest.of(0, 10);

    // when - 소문자로 검색
    Page<Thread> result = threadRepository.searchPublicThreadsByContent("public", pageable);

    // then - 대소문자 관계없이 모두 검색되어야 함
    assertThat(result.getContent()).hasSize(2);
    
    // 생성일 기준 내림차순 정렬 확인 (최신 순)
    List<Thread> threads = result.getContent();
    assertThat(threads)
        .extracting(Thread::getContent)
        .containsExactly("public content thread", "Public Content Thread");
    
    // 실제 생성일 순서 검증
    for (int i = 0; i < threads.size() - 1; i++) {
      assertThat(threads.get(i).getCreatedAt())
          .isAfterOrEqualTo(threads.get(i + 1).getCreatedAt());
    }
  }
}