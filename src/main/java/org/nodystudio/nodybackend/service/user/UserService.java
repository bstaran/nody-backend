package org.nodystudio.nodybackend.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.exception.custom.AccountAlreadyDeactivatedException;
import org.nodystudio.nodybackend.exception.custom.DuplicateNicknameException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.nodystudio.nodybackend.service.comment.CommentService;
import org.nodystudio.nodybackend.service.like.LikeService;
import org.nodystudio.nodybackend.service.log.LogService;
import org.nodystudio.nodybackend.service.thread.ThreadService;
import org.nodystudio.nodybackend.util.LoggingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final LogService logService;
  private final ThreadService threadService;
  private final CommentService commentService;
  private final LikeService likeService;

  public UserDetailResponseDto getCurrentUser(String userId) {
    User user = findUserById(userId);
    return UserDetailResponseDto.fromEntity(user);
  }

  @Transactional
  public UserDetailResponseDto updateNickname(String userId, UpdateNicknameRequestDto requestDto) {
    User user = findUserById(userId);

    log.info("사용자 닉네임 변경: userId={}", LoggingUtils.maskUserId(userId));
    log.debug("닉네임 변경 상세: 기존닉네임={}, 새닉네임={}",
        LoggingUtils.maskNickname(user.getNickname()),
        LoggingUtils.maskNickname(requestDto.getNickname()));

    // 닉네임 중복 검증 (본인 제외)
    if (userRepository.existsByNicknameAndIdNotAndIsActiveTrue(requestDto.getNickname(),
        user.getId())) {
      log.warn("닉네임 중복 시도: userId={}, nickname={}",
          LoggingUtils.maskUserId(userId), LoggingUtils.maskNickname(requestDto.getNickname()));
      throw new DuplicateNicknameException("이미 사용 중인 닉네임입니다: " + requestDto.getNickname());
    }

    user.updateNickname(requestDto.getNickname());

    return UserDetailResponseDto.fromEntity(user);
  }

  /**
   * 사용자 계정을 비활성화합니다 (회원탈퇴)
   *
   * @param userId 탈퇴할 사용자 ID
   * @throws UserNotFoundException              사용자를 찾을 수 없는 경우
   * @throws AccountAlreadyDeactivatedException 이미 탈퇴한 계정인 경우
   */
  @Transactional
  public void deactivateAccount(String userId) {
    User user = findUserByIdIncludingInactive(userId);

    if (!user.getIsActive()) {
      throw new AccountAlreadyDeactivatedException("이미 탈퇴한 계정입니다.");
    }

    log.info("사용자 계정 탈퇴 처리: userId={}", LoggingUtils.maskUserId(userId));
    log.debug("계정 탈퇴 상세: email={}, nickname={}",
        LoggingUtils.maskEmail(user.getEmail()),
        LoggingUtils.maskNickname(user.getNickname()));

    // 1. 사용자 생성 데이터 비활성화 (Soft Delete)
    deactivateUserGeneratedData(user);

    // 2. 계정 비활성화
    user.deactivateAccount();

    log.info("사용자 계정 탈퇴 완료: userId={}", LoggingUtils.maskUserId(userId));
  }

  /**
   * 사용자가 생성한 모든 데이터를 비활성화합니다. (Soft Delete) 30일 이내 재활성화 가능하도록 데이터는 보존하되 비공개 처리합니다.
   *
   * @param user 탈퇴하는 사용자
   */
  private void deactivateUserGeneratedData(User user) {
    log.debug("사용자 생성 데이터 비활성화 시작: userId={}", LoggingUtils.maskUserId(user.getId()));

    // 1. 사용자 로그를 비활성화하여 조회에서 제외 (원본 공개설정 보존)
    int deactivatedLogs = logService.deactivateLogsByUserId(user.getId());
    log.debug("로그 비활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        deactivatedLogs);

    // 2. 사용자 스레드를 비활성화하여 조회에서 제외 (원본 공개설정 보존)
    int deactivatedThreads = threadService.deactivateThreadsByUserId(user.getId());
    log.debug("스레드 비활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        deactivatedThreads);

    // 3. 사용자 댓글을 비활성화하여 조회에서 제외
    int deactivatedComments = commentService.deactivateCommentsByUserId(user.getId());
    log.debug("댓글 비활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        deactivatedComments);

    // 4. 사용자 좋아요를 비활성화하여 조회에서 제외
    int deactivatedLikes = likeService.deactivateLikesByUserId(user.getId());
    log.debug("좋아요 비활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        deactivatedLikes);

    log.info("사용자 생성 데이터 비활성화 완료: userId={}, logs={}, threads={}, comments={}, likes={}",
        LoggingUtils.maskUserId(user.getId()), deactivatedLogs, deactivatedThreads,
        deactivatedComments, deactivatedLikes);
  }

  /**
   * 비활성화된 사용자 데이터를 재활성화합니다. 탈퇴 취소 시 사용자가 생성한 콘텐츠를 다시 공개 처리합니다.
   *
   * @param user 재활성화할 사용자
   */
  public void reactivateUserGeneratedData(User user) {
    log.debug("사용자 생성 데이터 재활성화 시작: userId={}", LoggingUtils.maskUserId(user.getId()));

    // 1. 사용자 로그를 재활성화하여 원본 공개설정으로 복원
    int reactivatedLogs = logService.reactivateLogsByUserId(user.getId());
    log.debug("로그 재활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        reactivatedLogs);

    // 2. 사용자 스레드를 재활성화하여 원본 공개설정으로 복원
    int reactivatedThreads = threadService.reactivateThreadsByUserId(user.getId());
    log.debug("스레드 재활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        reactivatedThreads);

    // 3. 사용자 댓글을 재활성화하여 다시 조회 가능하도록 복원
    int reactivatedComments = commentService.reactivateCommentsByUserId(user.getId());
    log.debug("댓글 재활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        reactivatedComments);

    // 4. 사용자 좋아요를 재활성화하여 다시 조회 가능하도록 복원
    int reactivatedLikes = likeService.reactivateLikesByUserId(user.getId());
    log.debug("좋아요 재활성화 완료: userId={}, count={}", LoggingUtils.maskUserId(user.getId()),
        reactivatedLikes);

    log.info("사용자 생성 데이터 재활성화 완료: userId={}, logs={}, threads={}, comments={}, likes={}",
        LoggingUtils.maskUserId(user.getId()), reactivatedLogs, reactivatedThreads,
        reactivatedComments, reactivatedLikes);
  }

  /**
   * 사용자 ID로 사용자를 조회합니다.
   *
   * @param userId 사용자 ID (문자열)
   * @return 조회된 사용자 엔티티
   * @throws UserNotFoundException    사용자를 찾을 수 없는 경우
   * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
   */
  private User findUserById(String userId) {
    Long userIdLong = parseAndValidateUserId(userId);
    return userRepository.findByIdAndIsActiveTrue(userIdLong)
        .orElseThrow(() -> UserNotFoundException.byUserId(userId));
  }

  /**
   * 사용자 ID로 사용자를 조회합니다 (비활성 사용자 포함).
   *
   * @param userId 사용자 ID (문자열)
   * @return 조회된 사용자 엔티티
   * @throws UserNotFoundException    사용자를 찾을 수 없는 경우
   * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
   */
  private User findUserByIdIncludingInactive(String userId) {
    Long userIdLong = parseAndValidateUserId(userId);
    return userRepository.findById(userIdLong)
        .orElseThrow(() -> UserNotFoundException.byUserId(userId));
  }

  /**
   * 사용자 ID 문자열을 파싱하고 유효성을 검증합니다.
   *
   * @param userId 사용자 ID (문자열)
   * @return 파싱된 사용자 ID (Long)
   * @throws IllegalArgumentException 사용자 ID가 유효하지 않은 경우
   */
  private Long parseAndValidateUserId(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다.");
    }

    Long userIdLong;
    try {
      userIdLong = Long.parseLong(userId.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("유효하지 않은 사용자 ID 형식입니다: " + userId);
    }

    if (userIdLong <= 0) {
      throw new IllegalArgumentException("사용자 ID는 양수여야 합니다: " + userId);
    }

    return userIdLong;
  }
}