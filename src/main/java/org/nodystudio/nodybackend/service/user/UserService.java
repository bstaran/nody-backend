package org.nodystudio.nodybackend.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.dto.user.UpdateNicknameRequestDto;
import org.nodystudio.nodybackend.dto.user.UserDetailResponseDto;
import org.nodystudio.nodybackend.exception.custom.AccountAlreadyDeactivatedException;
import org.nodystudio.nodybackend.exception.custom.UserNotFoundException;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  public UserDetailResponseDto getCurrentUser(String userId) {
    User user = findUserById(userId);
    return UserDetailResponseDto.fromEntity(user);
  }

  @Transactional
  public UserDetailResponseDto updateNickname(String userId, UpdateNicknameRequestDto requestDto) {
    User user = findUserById(userId);

    log.info("사용자 닉네임 변경: userId={}, 기존닉네임={}, 새닉네임={}",
        userId, user.getNickname(), requestDto.getNickname());

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

    log.info("사용자 계정 탈퇴 처리: userId={}, email={}, nickname={}",
        userId, user.getEmail(), user.getNickname());

    // 1. 사용자 생성 데이터 삭제 (현재는 User만 존재, 추후 Log/Thread/Comment 추가 시 확장)
    deleteUserGeneratedData(user);

    // 2. 계정 비활성화
    user.deactivateAccount();

    log.info("사용자 계정 탈퇴 완료: userId={}", userId);
  }

  /**
   * 사용자가 생성한 모든 데이터를 비활성화합니다. (Soft Delete) 30일 이내 재활성화 가능하도록 데이터는 보존하되 비공개 처리합니다.
   *
   * @param user 탈퇴하는 사용자
   */
  private void deleteUserGeneratedData(User user) {
    log.debug("사용자 생성 데이터 비활성화 시작: userId={}", user.getId());

    // TODO: 아래 엔티티들이 구현되면 주석 해제
    // 1. Log 비활성화 (isVisible = false 설정)
    // logRepository.deactivateByUserId(user.getUserId());

    // 2. Thread 비활성화 (isPublic = false로 변경)
    // threadRepository.deactivateByUserId(user.getUserId());

    // 3. Comment 비활성화 (soft delete)
    // commentRepository.deactivateByUserId(user.getUserId());

    // 4. 좋아요 비활성화 (soft delete)
    // likeRepository.deactivateByUserId(user.getUserId());

    log.debug("사용자 생성 데이터 비활성화 완료: userId={}", user.getId());
  }

  /**
   * 비활성화된 사용자 데이터를 재활성화합니다. 탈퇴 취소 시 사용자가 생성한 콘텐츠를 다시 공개 처리합니다.
   *
   * @param user 재활성화할 사용자
   */
  public void reactivateUserGeneratedData(User user) {
    log.debug("사용자 생성 데이터 재활성화 시작: userId={}", user.getId());

    // TODO: 아래 엔티티들이 구현되면 주석 해제
    // 1. Log 재활성화 (isVisible = true 설정)
    // logRepository.reactivateByUserId(user.getUserId());

    // 2. Thread 재활성화 (원래 공개 설정으로 복원)
    // threadRepository.reactivateByUserId(user.getUserId());

    // 3. Comment 재활성화
    // commentRepository.reactivateByUserId(user.getUserId());

    // 4. 좋아요 데이터 재활성화
    // likeRepository.reactivateByUserId(user.getUserId());

    log.debug("사용자 생성 데이터 재활성화 완료: userId={}", user.getId());
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

    return userRepository.findById(userIdLong)
        .orElseThrow(() -> UserNotFoundException.byUserId(userId));
  }
}