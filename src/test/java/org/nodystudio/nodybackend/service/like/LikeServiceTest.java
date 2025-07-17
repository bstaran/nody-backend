package org.nodystudio.nodybackend.service.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nodystudio.nodybackend.domain.enums.OAuthProvider;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.domain.like.Like;
import org.nodystudio.nodybackend.domain.log.Log;
import org.nodystudio.nodybackend.domain.thread.Thread;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.like.LikeRequest;
import org.nodystudio.nodybackend.dto.like.LikeStatusResponse;
import org.nodystudio.nodybackend.exception.custom.AnonymousUserLikeNotAllowedException;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.LikeRepository;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService 테스트")
class LikeServiceTest {

  @Mock
  private LikeRepository likeRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ThreadRepository threadRepository;

  @Mock
  private LogRepository logRepository;

  @InjectMocks
  private LikeService likeService;

  private User testUser;
  private Thread testThread;
  private Log testLog;
  private LikeRequest threadLikeRequest;
  private LikeRequest logLikeRequest;

  @BeforeEach
  void setUp() {
    testUser = createUser(1L, "test@example.com", "testuser");
    testThread = createThread(1L, testUser);
    testLog = createLog(1L, testUser);

    threadLikeRequest = LikeRequest.builder()
        .targetType(TargetType.THREAD)
        .targetId(1L)
        .build();

    logLikeRequest = LikeRequest.builder()
        .targetType(TargetType.LOG)
        .targetId(1L)
        .build();
  }

  // 테스트 헬퍼 메서드들
  private User createUser(Long id, String email, String nickname) {
    User user = User.builder()
        .provider(OAuthProvider.GOOGLE)
        .socialId("12345")
        .email(email)
        .nickname(nickname)
        .build();

    // 리플렉션을 사용해 ID 설정
    try {
      java.lang.reflect.Field idField = User.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(user, id);
    } catch (Exception e) {
      // 테스트 환경에서 예외 발생 시 무시
    }

    return user;
  }

  private Thread createThread(Long id, User user) {
    Thread thread = Thread.builder()
        .user(user)
        .content("테스트 스레드 내용")
        .build();

    // 리플렉션을 사용해 ID 설정
    try {
      java.lang.reflect.Field idField = Thread.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(thread, id);
    } catch (Exception e) {
      // 테스트 환경에서 예외 발생 시 무시
    }

    return thread;
  }

  private Log createLog(Long id, User user) {
    Log log = Log.builder()
        .user(user)
        .content("테스트 로그 내용")
        .build();

    // 리플렉션을 사용해 ID 설정
    try {
      java.lang.reflect.Field idField = Log.class.getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(log, id);
    } catch (Exception e) {
      // 테스트 환경에서 예외 발생 시 무시
    }

    return log;
  }

  private Like createLike(User user, TargetType targetType, Long targetId) {
    return Like.builder()
        .user(user)
        .targetType(targetType)
        .targetId(targetId)
        .build();
  }

  @Nested
  @DisplayName("좋아요 토글 테스트")
  class ToggleLikeTest {

    @Test
    @DisplayName("스레드에 좋아요를 추가한다")
    void toggleLike_AddThreadLike_ShouldReturnLikedStatus() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.findById(1L)).willReturn(Optional.of(testThread));
      given(likeRepository.atomicToggleLike(1L, "THREAD", 1L)).willReturn(1);
      given(likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(1L, TargetType.THREAD, 1L))
          .willReturn(true);
      given(likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD, 1L))
          .willReturn(1L);

      // When
      LikeStatusResponse result = likeService.toggleLike(threadLikeRequest, "test@example.com");

      // Then
      assertThat(result.getIsLiked()).isTrue();
      assertThat(result.getLikeCount()).isEqualTo(1L);

      then(likeRepository).should(times(1)).atomicToggleLike(1L, "THREAD", 1L);
      then(likeRepository).should(never()).save(any(Like.class));
      then(likeRepository).should(never()).delete(any(Like.class));
    }

    @Test
    @DisplayName("로그에 좋아요를 추가한다")
    void toggleLike_AddLogLike_ShouldReturnLikedStatus() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(logRepository.findById(1L)).willReturn(Optional.of(testLog));
      given(likeRepository.atomicToggleLike(1L, "LOG", 1L)).willReturn(1);
      given(likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(1L, TargetType.LOG, 1L))
          .willReturn(true);
      given(likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.LOG, 1L))
          .willReturn(1L);

      // When
      LikeStatusResponse result = likeService.toggleLike(logLikeRequest, "test@example.com");

      // Then
      assertThat(result.getIsLiked()).isTrue();
      assertThat(result.getLikeCount()).isEqualTo(1L);

      then(likeRepository).should(times(1)).atomicToggleLike(1L, "LOG", 1L);
      then(likeRepository).should(never()).save(any(Like.class));
      then(likeRepository).should(never()).delete(any(Like.class));
    }

    @Test
    @DisplayName("스레드의 좋아요를 취소한다")
    void toggleLike_RemoveThreadLike_ShouldReturnUnlikedStatus() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.findById(1L)).willReturn(Optional.of(testThread));
      given(likeRepository.atomicToggleLike(1L, "THREAD", 1L)).willReturn(1);
      given(likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(1L, TargetType.THREAD, 1L))
          .willReturn(false);
      given(likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD, 1L))
          .willReturn(0L);

      // When
      LikeStatusResponse result = likeService.toggleLike(threadLikeRequest, "test@example.com");

      // Then
      assertThat(result.getIsLiked()).isFalse();
      assertThat(result.getLikeCount()).isEqualTo(0L);

      then(likeRepository).should(times(1)).atomicToggleLike(1L, "THREAD", 1L);
      then(likeRepository).should(never()).delete(any(Like.class));
      then(likeRepository).should(never()).save(any(Like.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자인 경우 예외를 발생시킨다")
    void toggleLike_WithNonExistentUser_ShouldThrowException() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> likeService.toggleLike(threadLikeRequest, "test@example.com"))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessage("사용자를 찾을 수 없습니다. Email: test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 스레드인 경우 예외를 발생시킨다")
    void toggleLike_WithNonExistentThread_ShouldThrowException() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(threadRepository.findById(1L)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> likeService.toggleLike(threadLikeRequest, "test@example.com"))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("스레드를 찾을 수 없습니다. ID: 1");
    }

    @Test
    @DisplayName("존재하지 않는 로그인 경우 예외를 발생시킨다")
    void toggleLike_WithNonExistentLog_ShouldThrowException() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(logRepository.findById(1L)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> likeService.toggleLike(logLikeRequest, "test@example.com"))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("로그를 찾을 수 없습니다. ID: 1");
    }

    @Test
    @DisplayName("익명 사용자(null 이메일)인 경우 예외를 발생시킨다")
    void toggleLike_WithNullEmail_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> likeService.toggleLike(threadLikeRequest, null))
          .isInstanceOf(AnonymousUserLikeNotAllowedException.class)
          .hasMessage("익명 사용자는 좋아요를 누를 수 없습니다.");
    }

    @Test
    @DisplayName("익명 사용자(빈 이메일)인 경우 예외를 발생시킨다")
    void toggleLike_WithEmptyEmail_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> likeService.toggleLike(threadLikeRequest, ""))
          .isInstanceOf(AnonymousUserLikeNotAllowedException.class)
          .hasMessage("익명 사용자는 좋아요를 누를 수 없습니다.");
    }

    @Test
    @DisplayName("익명 사용자(공백 이메일)인 경우 예외를 발생시킨다")
    void toggleLike_WithBlankEmail_ShouldThrowException() {
      // When & Then
      assertThatThrownBy(() -> likeService.toggleLike(threadLikeRequest, "   "))
          .isInstanceOf(AnonymousUserLikeNotAllowedException.class)
          .hasMessage("익명 사용자는 좋아요를 누를 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("좋아요 상태 조회 테스트")
  class GetLikeStatusTest {

    @Test
    @DisplayName("인증된 사용자의 좋아요 상태를 조회한다")
    void getLikeStatus_WithAuthenticatedUser_ShouldReturnCorrectStatus() {
      // Given
      given(threadRepository.findById(1L)).willReturn(Optional.of(testThread));
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD, 1L))
          .willReturn(5L);
      given(likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(1L, TargetType.THREAD, 1L))
          .willReturn(true);

      // When
      LikeStatusResponse result = likeService.getLikeStatus(TargetType.THREAD, 1L,
          "test@example.com");

      // Then
      assertThat(result.getIsLiked()).isTrue();
      assertThat(result.getLikeCount()).isEqualTo(5L);
      assertThat(result.getTargetType()).isEqualTo(TargetType.THREAD);
      assertThat(result.getTargetId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("익명 사용자의 좋아요 상태를 조회한다")
    void getLikeStatus_WithAnonymousUser_ShouldReturnCorrectStatus() {
      // Given
      given(threadRepository.findById(1L)).willReturn(Optional.of(testThread));
      given(likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD, 1L))
          .willReturn(3L);

      // When
      LikeStatusResponse result = likeService.getLikeStatus(TargetType.THREAD, 1L, null);

      // Then
      assertThat(result.getIsLiked()).isFalse();
      assertThat(result.getLikeCount()).isEqualTo(3L);
      assertThat(result.getTargetType()).isEqualTo(TargetType.THREAD);
      assertThat(result.getTargetId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 대상인 경우 예외를 발생시킨다")
    void getLikeStatus_WithNonExistentTarget_ShouldThrowException() {
      // Given
      given(threadRepository.findById(1L)).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> likeService.getLikeStatus(TargetType.THREAD, 1L, "test@example.com"))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("스레드를 찾을 수 없습니다. ID: 1");
    }
  }

  @Nested
  @DisplayName("좋아요 개수 조회 테스트")
  class GetLikeCountTest {

    @Test
    @DisplayName("활성 좋아요 개수를 정확히 조회한다")
    void getLikeCount_ShouldReturnCorrectCount() {
      // Given
      given(likeRepository.countByTargetTypeAndTargetIdAndIsActiveTrue(TargetType.THREAD, 1L))
          .willReturn(10L);

      // When
      long result = likeService.getLikeCount(TargetType.THREAD, 1L);

      // Then
      assertThat(result).isEqualTo(10L);
    }
  }

  @Nested
  @DisplayName("사용자별 좋아요 여부 확인 테스트")
  class IsLikedByUserTest {

    @Test
    @DisplayName("사용자가 활성 좋아요를 눌렀으면 true를 반환한다")
    void isLikedByUser_WithLike_ShouldReturnTrue() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(1L, TargetType.THREAD, 1L))
          .willReturn(true);

      // When
      boolean result = likeService.isLikedByUser(TargetType.THREAD, 1L, "test@example.com");

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("사용자가 활성 좋아요를 누르지 않았으면 false를 반환한다")
    void isLikedByUser_WithoutLike_ShouldReturnFalse() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
      given(likeRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsActiveTrue(1L, TargetType.THREAD, 1L))
          .willReturn(false);

      // When
      boolean result = likeService.isLikedByUser(TargetType.THREAD, 1L, "test@example.com");

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 사용자인 경우 예외를 발생시킨다")
    void isLikedByUser_WithNonExistentUser_ShouldThrowException() {
      // Given
      given(userRepository.findByEmail("test@example.com")).willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> likeService.isLikedByUser(TargetType.THREAD, 1L, "test@example.com"))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessage("사용자를 찾을 수 없습니다. Email: test@example.com");
    }
  }
}