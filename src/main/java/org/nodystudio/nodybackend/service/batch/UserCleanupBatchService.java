package org.nodystudio.nodybackend.service.batch;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nodystudio.nodybackend.domain.user.User;
import org.nodystudio.nodybackend.repository.CommentRepository;
import org.nodystudio.nodybackend.repository.LikeRepository;
import org.nodystudio.nodybackend.repository.LogRepository;
import org.nodystudio.nodybackend.repository.ThreadRepository;
import org.nodystudio.nodybackend.repository.UserRepository;
import org.nodystudio.nodybackend.util.LoggingUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 데이터 정리를 위한 배치 서비스
 * <p>
 * 탈퇴 후 30일이 지난 사용자 계정을 물리적으로 삭제합니다. 매일 새벽 2시에 실행되며, 관련된 모든 데이터(Log, Thread, Comment 등)도 함께 삭제됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupBatchService {

  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final ThreadRepository threadRepository;
  private final LogRepository logRepository;
  private final LikeRepository likeRepository;

  /**
   * 탈퇴 후 30일이 지난 사용자 계정을 완전 삭제합니다.
   * <p>
   * 매일 새벽 2시에 실행됩니다. - cron = "0 0 2 * * *" (초 분 시 일 월 요일) - 0초 0분 2시 매일 매월 모든요일
   */
  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void deleteExpiredDeactivatedUsers() {
    log.info("탈퇴한 사용자 정리 배치 작업 시작");

    try {
      // 30일 전 날짜 계산
      LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

      // 30일 이전에 탈퇴한 사용자 조회
      List<User> expiredUsers = userRepository.findByIsActiveFalseAndDeletedAtBefore(cutoffDate);

      if (expiredUsers.isEmpty()) {
        log.info("삭제할 만료된 사용자 계정이 없습니다.");
        return;
      }

      log.info("삭제 대상 사용자 수: {} 명", expiredUsers.size());

      // 사용자별로 삭제 처리
      int deletedCount = 0;
      for (User user : expiredUsers) {
        try {
          deleteUserCompletely(user);
          deletedCount++;
          log.info("사용자 계정 완전 삭제 완료: userId={}", LoggingUtils.maskUserId(user.getId()));
          log.debug("사용자 계정 완전 삭제 상세: userId={}, email={}",
              LoggingUtils.maskUserId(user.getId()), LoggingUtils.maskEmail(user.getEmail()));
        } catch (Exception e) {
          log.error("사용자 계정 삭제 중 오류 발생: userId={}, error={}",
              LoggingUtils.maskUserId(user.getId()), e.getMessage());
          log.debug("사용자 계정 삭제 오류 상세: userId={}, email={}",
              LoggingUtils.maskUserId(user.getId()), LoggingUtils.maskEmail(user.getEmail()), e);
        }
      }

      log.info("탈퇴한 사용자 정리 배치 작업 완료. 삭제된 계정: {} / {} 명",
          deletedCount, expiredUsers.size());

    } catch (Exception e) {
      log.error("탈퇴한 사용자 정리 배치 작업 중 오류 발생", e);
    }
  }

  /**
   * 탈퇴 후 30일이 지난 사용자와 관련된 모든 데이터를 완전히 삭제합니다. Soft Delete로 비활성화된 데이터들을 물리적으로 완전 삭제합니다.
   * <p>
   * 삭제 순서: 1. Comment (댓글) - 비활성화된 댓글들 삭제 2. Thread (쓰레드) - 비활성화된 쓰레드들 삭제 (CASCADE로 관련 댓글, 좋아요도 함께 삭제) 
   * 3. Log (로그) - 비활성화된 로그들 삭제 4. Like (좋아요) - 사용자의 모든 좋아요 삭제 5. User (사용자) - 탈퇴한 사용자 계정 삭제
   *
   * @param user 완전 삭제할 사용자 (탈퇴 후 30일 경과)
   */
  @Transactional
  protected void deleteUserCompletely(User user) {
    Long userId = user.getId();

    // 1. 사용자가 작성한 비활성화된 댓글들 완전 삭제
    int deletedComments = commentRepository.deleteDeactivatedByUserId(userId);
    log.info("사용자 댓글 완전 삭제 완료: userId={}, count={}", LoggingUtils.maskUserId(userId), deletedComments);

    // 2. 사용자가 작성한 비활성화된 Thread 관련 데이터 완전 삭제
    // Thread 삭제 시 CASCADE 설정에 의해 관련 댓글, 좋아요도 함께 삭제됨
    int deletedThreads = threadRepository.deleteDeactivatedByUserId(userId);
    log.info("사용자 스레드 완전 삭제 완료: userId={}, count={}", LoggingUtils.maskUserId(userId), deletedThreads);

    // 3. 사용자가 작성한 비활성화된 Log 관련 데이터 완전 삭제
    int deletedLogs = logRepository.deleteDeactivatedByUserId(userId);
    log.info("사용자 로그 완전 삭제 완료: userId={}, count={}", LoggingUtils.maskUserId(userId), deletedLogs);

    // 4. 사용자의 모든 좋아요 데이터 완전 삭제
    int deletedLikes = likeRepository.deleteByUserId(userId);
    log.info("사용자 좋아요 완전 삭제 완료: userId={}, count={}", LoggingUtils.maskUserId(userId), deletedLikes);

    // 5. 탈퇴한 사용자 계정 완전 삭제
    userRepository.delete(user);

    log.info("탈퇴 후 30일 경과 사용자 관련 모든 데이터 완전 삭제 완료: userId={}, comments={}, threads={}, logs={}, likes={}", 
        LoggingUtils.maskUserId(userId), deletedComments, deletedThreads, deletedLogs, deletedLikes);
  }

  /**
   * 수동으로 만료된 사용자 정리 작업을 실행합니다. 관리자가 필요시 호출할 수 있는 메서드입니다.
   *
   * @return 삭제된 사용자 수
   */
  @Transactional
  public int manualCleanupExpiredUsers() {
    log.info("수동 사용자 정리 작업 시작");

    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
    List<User> expiredUsers = userRepository.findByIsActiveFalseAndDeletedAtBefore(cutoffDate);

    if (expiredUsers.isEmpty()) {
      log.info("삭제할 만료된 사용자 계정이 없습니다.");
      return 0;
    }

    int deletedCount = 0;
    for (User user : expiredUsers) {
      try {
        deleteUserCompletely(user);
        deletedCount++;
      } catch (Exception e) {
        log.error("사용자 계정 삭제 중 오류 발생: userId={}, error={}",
            LoggingUtils.maskUserId(user.getId()), e.getMessage());
        log.debug("사용자 계정 삭제 오류 상세", e);
      }
    }

    log.info("수동 사용자 정리 작업 완료. 삭제된 계정: {} / {} 명",
        deletedCount, expiredUsers.size());

    return deletedCount;
  }


  /**
   * 탈퇴 예정 사용자 수를 조회합니다.
   *
   * @param daysUntilDeletion 삭제까지 남은 일수
   * @return 해당 일수 내에 삭제될 사용자 수
   */
  @Transactional(readOnly = true)
  public long countUsersToBeDeleted(int daysUntilDeletion) {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30 - daysUntilDeletion);
    return userRepository.countByIsActiveFalseAndDeletedAtBefore(cutoffDate);
  }
}