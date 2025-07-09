package org.nodystudio.nodybackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
@DisplayName("ThreadRepository 집계 테스트")
class ThreadRepositoryCountTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ThreadRepository threadRepository;

  private User user1;
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

    User user2 = User.builder()
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

    // 다른 사용자 스레드 (카운트 검증용)
    Thread otherUserThread = Thread.builder()
        .user(user2)
        .content("다른 사용자 스레드")
        .isPublic(true)
        .viewCount(1L)
        .build();
    entityManager.persistAndFlush(otherUserThread);

    entityManager.clear();
  }

  @Test
  @DisplayName("특정 로그의 스레드 개수 조회 - 전체 스레드 개수")
  void countByLogId_WhenLogIdExists_ShouldReturnTotalThreadCount() {
    // when
    long count = threadRepository.countByLogId(log1.getId());

    // then
    assertThat(count).isEqualTo(2); // log1에 연결된 스레드 2개 (공개 1개 + 비공개 1개)
  }

  @Test
  @DisplayName("특정 사용자의 스레드 개수 조회 - 사용자 작성 스레드 개수")
  void countByUserId_WhenUserIdExists_ShouldReturnUserThreadCount() {
    // when
    long count = threadRepository.countByUserId(user1.getId());

    // then
    assertThat(count).isEqualTo(2); // user1이 작성한 스레드 2개
  }

  @Test
  @DisplayName("존재하지 않는 로그의 스레드 개수 조회 - 0 반환")
  void countByLogId_WhenLogIdDoesNotExist_ShouldReturnZero() {
    // when
    long count = threadRepository.countByLogId(Long.MAX_VALUE);

    // then
    assertThat(count).isZero();
  }

  @Test
  @DisplayName("존재하지 않는 사용자의 스레드 개수 조회 - 0 반환")
  void countByUserId_WhenUserIdDoesNotExist_ShouldReturnZero() {
    // when
    long count = threadRepository.countByUserId(Long.MAX_VALUE);

    // then
    assertThat(count).isZero();
  }
}