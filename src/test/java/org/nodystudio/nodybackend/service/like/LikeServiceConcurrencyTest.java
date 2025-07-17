package org.nodystudio.nodybackend.service.like;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.domain.like.Like;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.like.LikeRequest;
import org.nodystudio.nodybackend.repository.LikeRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * LikeService 동시성 테스트 (H2 환경)
 *
 * <p>
 * H2 데이터베이스 환경에서 좋아요 토글 기능의 동시성 안전성을 검증합니다.
 * 일반적인 JPA 기반 동시성 테스트를 수행합니다.
 * </p>
 *
 * <p>
 * <strong>주의:</strong> 이 테스트는 H2 데이터베이스 환경에서 실행됩니다.
 * MySQL 전용 원자적 토글 기능은 {@code LikeServiceConcurrencyMySQLTest}에서 테스트됩니다.
 * </p>
 *
 * <p>
 * <strong>테스트 범위:</strong>
 * <ul>
 * <li>H2 환경에서의 기본적인 동시성 테스트</li>
 * <li>JPA 기반 동시성 처리 검증</li>
 * <li>H2 데이터베이스 제약사항 하에서의 동시성 안전성</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("LikeService 동시성 테스트 (H2 환경)")
@Disabled("H2 환경에서는 MySQL 전용 atomicToggleLike 쿼리를 지원하지 않음. MySQL 동시성 테스트는 LikeServiceConcurrencyMySQLTest 참조")
class LikeServiceConcurrencyTest {

  @Autowired
  private LikeService likeService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ThreadRepository threadRepository;

  @Autowired
  private LikeRepository likeRepository;

  private User testUser;
  private Thread testThread;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = User.builder()
        .provider(OAuthProvider.GOOGLE)
        .socialId("concurrent-test-user")
        .email("concurrent@test.com")
        .nickname("동시성테스트")
        .build();
    testUser = userRepository.save(testUser);

    // 테스트 스레드 생성
    testThread = Thread.builder()
        .user(testUser)
        .content("동시성 테스트용 스레드")
        .build();
    testThread = threadRepository.save(testThread);
  }

  @Test
  @DisplayName("동시에 여러 좋아요 추가 요청이 들어와도 원자적으로 처리된다")
  void concurrentLikeCreation_ShouldHandleAtomically() throws InterruptedException {
    // Given
    int numberOfThreads = 10;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    LikeRequest request = LikeRequest.builder()
        .targetType(TargetType.THREAD)
        .targetId(testThread.getId())
        .build();

    // When
    IntStream.range(0, numberOfThreads).forEach(i -> {
      executorService.submit(() -> {
        try {
          likeService.toggleLike(request, testUser.getEmail());
          successCount.incrementAndGet();
        } catch (Exception e) {
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    });

    latch.await(10, TimeUnit.SECONDS);
    executorService.shutdown();

    // 모든 스레드가 완전히 종료될 때까지 대기
    boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(terminated).isTrue();

    // Then
    // 원자적 연산이므로 모든 요청이 성공해야 함
    assertThat(successCount.get()).isEqualTo(numberOfThreads);
    assertThat(failureCount.get()).isEqualTo(0);

    // 최종 상태 확인 - 짝수 번 토글하면 좋아요가 없고, 홀수 번이면 있어야 함
    List<Like> allLikes = likeRepository.findAll();
    long activeLikes = allLikes.stream()
        .filter(like -> like.getIsActive())
        .count();

    // 10번(짝수)의 토글이므로 최종적으로 좋아요가 없어야 함
    assertThat(activeLikes).isEqualTo(0);

    // 하지만 데이터베이스에는 하나의 레코드가 존재해야 함 (isActive=false)
    assertThat(allLikes).hasSize(1);
    assertThat(allLikes.get(0).getIsActive()).isFalse();
  }

  @Test
  @DisplayName("좋아요가 있는 상태에서 동시에 토글 요청이 들어와도 최종 상태가 일관성있게 유지된다")
  void concurrentToggle_ShouldMaintainConsistency() throws InterruptedException {
    // Given - 먼저 좋아요를 추가
    Like existingLike = Like.builder()
        .user(testUser)
        .targetType(TargetType.THREAD)
        .targetId(testThread.getId())
        .isActive(true)
        .build();
    likeRepository.save(existingLike);

    int numberOfThreads = 20;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    LikeRequest request = LikeRequest.builder()
        .targetType(TargetType.THREAD)
        .targetId(testThread.getId())
        .build();

    // When - 동시에 토글 요청
    IntStream.range(0, numberOfThreads).forEach(i -> {
      executorService.submit(() -> {
        try {
          likeService.toggleLike(request, testUser.getEmail());
          successCount.incrementAndGet();
        } catch (Exception e) {
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    });

    latch.await(10, TimeUnit.SECONDS);
    executorService.shutdown();

    // 모든 스레드가 완전히 종료될 때까지 대기
    boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(terminated).isTrue();

    // Then
    // 모든 요청이 성공해야 함
    assertThat(successCount.get()).isEqualTo(numberOfThreads);
    assertThat(failureCount.get()).isEqualTo(0);

    // 최종 상태 확인 - 초기에 활성 좋아요가 1개 있었고, 20번 토글했으므로
    // 21번의 상태 변화가 있어야 함 (홀수이므로 최종적으로 활성)
    long activeLikeCount = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(
        TargetType.THREAD, testThread.getId());
    assertThat(activeLikeCount).isEqualTo(1);

    // 데이터베이스에는 여전히 하나의 레코드만 있어야 함
    List<Like> allLikes = likeRepository.findAll();
    assertThat(allLikes).hasSize(1);
    assertThat(allLikes.get(0).getIsActive()).isTrue();
  }

  @Test
  @DisplayName("서로 다른 사용자가 동시에 같은 대상에 좋아요를 누르면 모두 성공한다")
  void concurrentLikesByDifferentUsers_ShouldAllSucceed() throws InterruptedException {
    // Given
    int numberOfUsers = 5;
    List<User> users = IntStream.range(0, numberOfUsers)
        .mapToObj(i -> User.builder()
            .provider(OAuthProvider.GOOGLE)
            .socialId("user-" + i)
            .email("user" + i + "@test.com")
            .nickname("사용자" + i)
            .build())
        .map(userRepository::save)
        .toList();

    ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);
    CountDownLatch latch = new CountDownLatch(numberOfUsers);

    LikeRequest request = LikeRequest.builder()
        .targetType(TargetType.THREAD)
        .targetId(testThread.getId())
        .build();

    // When
    users.forEach(user -> {
      executorService.submit(() -> {
        try {
          likeService.toggleLike(request, user.getEmail());
        } finally {
          latch.countDown();
        }
      });
    });

    latch.await(10, TimeUnit.SECONDS);
    executorService.shutdown();

    // 모든 스레드가 완전히 종료될 때까지 대기
    boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(terminated).isTrue();

    // Then
    // 모든 사용자의 활성 좋아요가 정상적으로 생성되었는지 확인
    long totalActiveLikes = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(
        TargetType.THREAD, testThread.getId());
    assertThat(totalActiveLikes).isEqualTo(numberOfUsers);

    // 각 사용자별로 정확히 하나의 활성 좋아요가 생성되었는지 확인
    users.forEach(user -> {
      boolean liked = likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(
          user.getId(), TargetType.THREAD, testThread.getId());
      assertThat(liked).isTrue();
    });
  }

  @Test
  @DisplayName("동일 사용자가 빠르게 연속으로 토글 요청을 보내도 데이터 무결성이 유지된다")
  void rapidToggleByOneUser_ShouldMaintainIntegrity() throws InterruptedException {
    // Given
    int numberOfRequests = 50;
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(numberOfRequests);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    LikeRequest request = LikeRequest.builder()
        .targetType(TargetType.THREAD)
        .targetId(testThread.getId())
        .build();

    // When
    IntStream.range(0, numberOfRequests).forEach(i -> {
      executorService.submit(() -> {
        try {
          likeService.toggleLike(request, testUser.getEmail());
          successCount.incrementAndGet();
        } catch (Exception e) {
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    });

    latch.await(30, TimeUnit.SECONDS);
    executorService.shutdown();

    // 모든 스레드가 완전히 종료될 때까지 대기
    boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
    assertThat(terminated).isTrue();

    // Then
    // 모든 요청이 성공해야 함
    assertThat(successCount.get()).isEqualTo(numberOfRequests);
    assertThat(failureCount.get()).isEqualTo(0);

    // 최종 상태는 0 또는 1이어야 함 (짝수 번 토글하면 0, 홀수 번이면 1)
    // 50번(짝수)이므로 최종적으로 활성 좋아요는 0개여야 함
    long activeFinalCount = likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(
        TargetType.THREAD, testThread.getId());
    assertThat(activeFinalCount).isEqualTo(0);

    // 해당 사용자의 좋아요 레코드는 정확히 1개만 존재해야 함 (isActive=false)
    List<Like> userLikes = likeRepository.findAll().stream()
        .filter(like -> like.getUser().getId().equals(testUser.getId()))
        .toList();
    assertThat(userLikes).hasSize(1);
    assertThat(userLikes.get(0).getIsActive()).isFalse();
  }
}