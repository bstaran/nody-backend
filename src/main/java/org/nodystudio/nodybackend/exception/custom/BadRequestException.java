package org.nodystudio.nodybackend.exception.custom;

import org.nodystudio.nodybackend.dto.code.ErrorCode;

/**
 * 잘못된 요청 예외 클래스
 *
 * <p>
 * 클라이언트의 요청이 잘못되었을 때 발생하는 예외입니다. 예: 유효하지 않은 파라미터, 논리적으로 불가능한 요청 등
 * </p>
 */
public class BadRequestException extends BusinessException {

  /**
   * 기본 에러 메시지로 예외 생성
   */
  public BadRequestException() {
    super(ErrorCode.INVALID_INPUT_VALUE);
  }

  /**
   * 커스텀 메시지로 예외 생성
   *
   * @param message 예외 메시지
   */
  public BadRequestException(String message) {
    super(message, ErrorCode.INVALID_INPUT_VALUE);
  }

  /**
   * 커스텀 에러 코드로 예외 생성
   *
   * @param errorCode 에러 코드
   */
  public BadRequestException(ErrorCode errorCode) {
    super(errorCode);
  }

  /**
   * 커스텀 메시지와 에러 코드로 예외 생성
   *
   * @param message   예외 메시지
   * @param errorCode 에러 코드
   */
  public BadRequestException(String message, ErrorCode errorCode) {
    super(message, errorCode);
  }

  /**
   * 원인 예외와 함께 예외 생성
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public BadRequestException(String message, Throwable cause) {
    super(message, ErrorCode.INVALID_INPUT_VALUE, cause);
  }
}