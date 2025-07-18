package org.nodystudio.nodybackend.service.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.enums.TargetType;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.like.LikeRequest;
import org.nodystudio.nodybackend.dto.like.LikeStatusResponse;
import org.nodystudio.nodybackend.exception.custom.AnonymousUserLikeNotAllowedException;
import org.nodystudio.nodybackend.exception.custom.ResourceNotFoundException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.LogLikeRepository;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.ThreadLikeRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.nodystudio.nodybackend.util.LoggingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 중간 엔티티 기반 좋아요 관리 서비스
 *
 * <p>
 * ThreadLike와 LogLike 중간 엔티티를 사용하여 참조 무결성을 보장하고
 * 타입 안전성을 제공하는 좋아요 시스템입니다.
 * 스레드와 로그에 대한 좋아요 생성, 취소, 조회 기능을 제공합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

  private final ThreadLikeRepository threadLikeRepository;
  private final LogLikeRepository logLikeRepository;
  private final UserRepository userRepository;
  private final ThreadRepository threadRepository;
  private final LogRepository logRepository;

  /**
   * 좋아요를 토글합니다. (좋아요 추가/취소)
   * 원자적 연산을 사용하여 동시성 문제를 근본적으로 해결합니다.
   *
   * @param request   좋아요 요청 정보
   * @param userEmail 사용자 이메일
   * @return 좋아요 상태 정보
   * @throws AnonymousUserLikeNotAllowedException 익명 사용자가 좋아요를 시도한 경우
   * @throws UserNotFoundException                사용자를 찾을 수 없는 경우
   * @throws ResourceNotFoundException            대상을 찾을 수 없는 경우
   */
  @Transactional
  public LikeStatusResponse toggleLike(LikeRequest request, String userEmail) {
    log.info("좋아요 토글 시작 - userEmail: {}, targetType: {}, targetId: {}",
        userEmail, request.getTargetType(), request.getTargetId());

    // 익명 사용자 검증
    if (userEmail == null || userEmail.trim().isEmpty()) {
      log.warn("익명 사용자 좋아요 시도 - targetType: {}, targetId: {}",
          request.getTargetType(), request.getTargetId());
      throw new AnonymousUserLikeNotAllowedException("익명 사용자는 좋아요를 누를 수 없습니다.");
    }

    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. Email: " + userEmail));

    validateTargetExists(request.getTargetType(), request.getTargetId());

    // 타입별 원자적 토글 연산 실행
    int affectedRows = switch (request.getTargetType()) {
      case THREAD -> threadLikeRepository.atomicToggleLike(user.getId(), request.getTargetId());
      case LOG -> logLikeRepository.atomicToggleLike(user.getId(), request.getTargetId());
    };

    log.info("원자적 토글 완료 - userId: {}, targetType: {}, targetId: {}, affectedRows: {}",
        user.getId(), request.getTargetType(), request.getTargetId(), affectedRows);

    // 결과 조회
    boolean isLiked = switch (request.getTargetType()) {
      case THREAD -> threadLikeRepository.existsByUserIdAndThreadIdAndIsActiveTrue(
          user.getId(), request.getTargetId());
      case LOG -> logLikeRepository.existsByUserIdAndLogIdAndIsActiveTrue(
          user.getId(), request.getTargetId());
    };

    long likeCount = switch (request.getTargetType()) {
      case THREAD -> threadLikeRepository.countByThreadIdAndIsActiveTrue(request.getTargetId());
      case LOG -> logLikeRepository.countByLogIdAndIsActiveTrue(request.getTargetId());
    };

    log.info("좋아요 토글 완료 - userId: {}, targetType: {}, targetId: {}, isLiked: {}, likeCount: {}",
        user.getId(), request.getTargetType(), request.getTargetId(), isLiked, likeCount);

    return LikeStatusResponse.of(isLiked, likeCount, request.getTargetType(),
        request.getTargetId());
  }

  /**
   * 좋아요 상태를 조회합니다.
   *
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @param userEmail  사용자 이메일 (null 가능)
   * @return 좋아요 상태 정보
   * @throws ResourceNotFoundException 대상을 찾을 수 없는 경우
   */
  public LikeStatusResponse getLikeStatus(TargetType targetType, Long targetId, String userEmail) {
    log.info("좋아요 상태 조회 시작 - targetType: {}, targetId: {}, userEmail: {}",
        targetType, targetId, userEmail);

    // 대상 존재 확인
    validateTargetExists(targetType, targetId);

    // 활성 좋아요 개수 조회
    long likeCount = switch (targetType) {
      case THREAD -> threadLikeRepository.countByThreadIdAndIsActiveTrue(targetId);
      case LOG -> logLikeRepository.countByLogIdAndIsActiveTrue(targetId);
    };

    boolean isLiked = false;
    if (userEmail != null) {
      // 사용자별 좋아요 상태 확인
      User user = userRepository.findByEmail(userEmail)
          .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. Email: " + userEmail));

      isLiked = switch (targetType) {
        case THREAD -> threadLikeRepository.existsByUserIdAndThreadIdAndIsActiveTrue(
            user.getId(), targetId);
        case LOG -> logLikeRepository.existsByUserIdAndLogIdAndIsActiveTrue(
            user.getId(), targetId);
      };
    }

    log.info("좋아요 상태 조회 완료 - targetType: {}, targetId: {}, isLiked: {}, likeCount: {}",
        targetType, targetId, isLiked, likeCount);

    return LikeStatusResponse.of(isLiked, likeCount, targetType, targetId);
  }

  /**
   * 특정 대상의 활성 좋아요 개수를 조회합니다.
   *
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @return 활성 좋아요 개수
   */
  public long getLikeCount(TargetType targetType, Long targetId) {
    log.debug("활성 좋아요 개수 조회 - targetType: {}, targetId: {}", targetType, targetId);
    return switch (targetType) {
      case THREAD -> threadLikeRepository.countByThreadIdAndIsActiveTrue(targetId);
      case LOG -> logLikeRepository.countByLogIdAndIsActiveTrue(targetId);
    };
  }

  /**
   * 사용자가 특정 대상에 활성 좋아요를 눌렀는지 확인합니다.
   *
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @param userEmail  사용자 이메일
   * @return 활성 좋아요 여부
   * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
   */
  public boolean isLikedByUser(TargetType targetType, Long targetId, String userEmail) {
    log.debug("사용자 활성 좋아요 여부 확인 - targetType: {}, targetId: {}, userEmail: {}",
        targetType, targetId, userEmail);

    User user = userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. Email: " + userEmail));

    return switch (targetType) {
      case THREAD -> threadLikeRepository.existsByUserIdAndThreadIdAndIsActiveTrue(
          user.getId(), targetId);
      case LOG -> logLikeRepository.existsByUserIdAndLogIdAndIsActiveTrue(
          user.getId(), targetId);
    };
  }

  /**
   * 대상이 존재하는지 검증합니다.
   *
   * @param targetType 대상 타입
   * @param targetId   대상 ID
   * @throws ResourceNotFoundException 대상을 찾을 수 없는 경우
   */
  private void validateTargetExists(TargetType targetType, Long targetId) {
    switch (targetType) {
      case THREAD:
        threadRepository.findById(targetId)
            .orElseThrow(() -> new ResourceNotFoundException("스레드를 찾을 수 없습니다. ID: " + targetId));
        log.debug("스레드 존재 확인 완료 - threadId: {}", targetId);
        break;
      case LOG:
        logRepository.findById(targetId)
            .orElseThrow(() -> new ResourceNotFoundException("로그를 찾을 수 없습니다. ID: " + targetId));
        log.debug("로그 존재 확인 완료 - logId: {}", targetId);
        break;
      default:
        throw new IllegalArgumentException("지원하지 않는 대상 타입입니다: " + targetType);
    }
  }

  /**
   * 특정 사용자의 모든 활성 좋아요를 비활성화합니다.
   * 계정 탈퇴 시 사용자 생성 데이터를 안전하게 보존하면서 조회에서 제외시킵니다.
   *
   * @param userId 비활성화할 사용자 ID
   * @return 비활성화된 좋아요의 개수
   */
  @Transactional
  public int deactivateLikesByUserId(Long userId) {
    log.debug("사용자 좋아요 비활성화 시작: userId={}", LoggingUtils.maskUserId(userId));

    int threadLikesDeactivated = threadLikeRepository.deactivateByUserId(userId);
    int logLikesDeactivated = logLikeRepository.deactivateByUserId(userId);
    int totalDeactivated = threadLikesDeactivated + logLikesDeactivated;

    log.debug("사용자 좋아요 비활성화 완료: userId={}, threadLikes={}, logLikes={}, total={}",
        LoggingUtils.maskUserId(userId), threadLikesDeactivated, logLikesDeactivated,
        totalDeactivated);

    return totalDeactivated;
  }

  /**
   * 특정 사용자의 모든 비활성화된 좋아요를 재활성화합니다.
   * 계정 복구 시 다시 조회 가능하도록 복원합니다.
   *
   * @param userId 재활성화할 사용자 ID
   * @return 재활성화된 좋아요의 개수
   */
  @Transactional
  public int reactivateLikesByUserId(Long userId) {
    log.debug("사용자 좋아요 재활성화 시작: userId={}", LoggingUtils.maskUserId(userId));

    int threadLikesReactivated = threadLikeRepository.reactivateByUserId(userId);
    int logLikesReactivated = logLikeRepository.reactivateByUserId(userId);
    int totalReactivated = threadLikesReactivated + logLikesReactivated;

    log.debug("사용자 좋아요 재활성화 완료: userId={}, threadLikes={}, logLikes={}, total={}",
        LoggingUtils.maskUserId(userId), threadLikesReactivated, logLikesReactivated,
        totalReactivated);

    return totalReactivated;
  }
}