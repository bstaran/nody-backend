package org.nodystudio.nodybackend.exception.custom;

import java.time.LocalDateTime;
import org.nodystudio.nodybackend.dto.code.ErrorCode;

/**
 * 재가입 제한 예외
 * <p>
 * 30일 이내에 탈퇴한 이메일로 재가입을 시도할 때 발생합니다.
 */
public class ReRegistrationRestrictedException extends BusinessException {

  private final String email;
  private final LocalDateTime deletedAt;

  public ReRegistrationRestrictedException(String email, LocalDateTime deletedAt) {
    super(String.format("해당 이메일(%s)로는 탈퇴 후 30일 동안 재가입할 수 없습니다. 30일 후에 다시 시도해주세요.", email),
        ErrorCode.REREGISTRATION_RESTRICTED);
    this.email = email;
    this.deletedAt = deletedAt;
  }

  public ReRegistrationRestrictedException(String email, LocalDateTime deletedAt, String message) {
    super(message, ErrorCode.REREGISTRATION_RESTRICTED);
    this.email = email;
    this.deletedAt = deletedAt;
  }

  public String getEmail() {
    return email;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  /**
   * 재가입 가능한 날짜를 계산합니다.
   *
   * @return 재가입 가능한 날짜
   */
  public LocalDateTime getReRegistrationAvailableDate() {
    return deletedAt != null ? deletedAt.plusDays(30) : null;
  }
}