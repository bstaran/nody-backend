package org.nodystudio.nodybackend.exception.custom;

import org.nodystudio.nodybackend.dto.code.ErrorCode;

/**
 * 중복된 닉네임 사용 시 발생하는 예외
 */
public class DuplicateNicknameException extends BusinessException {

  /**
   * 중복 닉네임 예외 생성자 (기본 메시지)
   */
  public DuplicateNicknameException() {
    super(ErrorCode.VALIDATION_ERROR);
  }

  /**
   * 중복 닉네임 예외 생성자 (커스텀 메시지)
   *
   * @param message 예외 메시지
   */
  public DuplicateNicknameException(String message) {
    super(message, ErrorCode.VALIDATION_ERROR);
  }

  /**
   * 중복 닉네임 예외 생성자 (커스텀 메시지 및 원인)
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public DuplicateNicknameException(String message, Throwable cause) {
    super(message, ErrorCode.VALIDATION_ERROR, cause);
  }
}