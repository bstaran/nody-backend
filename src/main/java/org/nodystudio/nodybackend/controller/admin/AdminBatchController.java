package org.nodystudio.nodybackend.controller.admin;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nodystudio.nodybackend.dto.ApiResponse;
import org.nodystudio.nodybackend.dto.code.SuccessCode;
import org.nodystudio.nodybackend.service.batch.UserCleanupBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 배치 작업 컨트롤러
 * <p>
 * 배치 작업의 수동 실행 및 모니터링 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class AdminBatchController {

  private final UserCleanupBatchService userCleanupBatchService;

  /**
   * 만료된 탈퇴 사용자 정리 작업을 수동으로 실행합니다
   *
   * @return 삭제된 사용자 수
   */
  @PostMapping("/user-cleanup")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> manualUserCleanup() {
    int deletedCount = userCleanupBatchService.manualCleanupExpiredUsers();

    Map<String, Object> result = Map.of(
        "deletedUserCount", deletedCount,
        "message", deletedCount > 0 ?
            deletedCount + "명의 만료된 사용자 계정이 삭제되었습니다." :
            "삭제할 만료된 사용자 계정이 없습니다."
    );

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, result));
  }

  /**
   * 삭제 예정 사용자 수를 조회합니다
   *
   * @param days 삭제까지 남은 일수 (기본값: 0일 = 즉시 삭제 대상)
   * @return 삭제 예정 사용자 수
   */
  @GetMapping("/user-cleanup/count")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getUserCleanupCount(
      @RequestParam(value = "days", defaultValue = "0") int days) {

    if (days < 0 || days > 30) {
      days = 0; // 유효하지 않은 값은 기본값으로 설정
    }

    long userCount = userCleanupBatchService.countUsersToBeDeleted(days);

    Map<String, Object> result = Map.of(
        "usersToBeDeleted", userCount,
        "daysUntilDeletion", days,
        "message", days == 0 ?
            "현재 삭제 대상 사용자 수입니다." :
            days + "일 후 삭제될 사용자 수입니다."
    );

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, result));
  }

  /**
   * 사용자 정리 배치 작업 상태 정보를 조회합니다
   *
   * @return 배치 작업 상태 정보
   */
  @GetMapping("/user-cleanup/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getUserCleanupStatus() {
    long immediateCount = userCleanupBatchService.countUsersToBeDeleted(0);
    long weekCount = userCleanupBatchService.countUsersToBeDeleted(7);
    long monthCount = userCleanupBatchService.countUsersToBeDeleted(30);

    Map<String, Object> result = Map.of(
        "immediateCleanup", immediateCount,
        "weeklyCleanup", weekCount,
        "monthlyCleanup", monthCount,
        "scheduleInfo", Map.of(
            "cronExpression", "0 0 2 * * *",
            "description", "매일 새벽 2시에 30일 이전 탈퇴 사용자 자동 삭제",
            "timezone", "시스템 기본 시간대"
        )
    );

    return ResponseEntity
        .status(SuccessCode.OK.getStatus())
        .body(ApiResponse.success(SuccessCode.OK, result));
  }
}