package org.nodystudio.nodybackend.exception.custom;

/**
 * 이미 활성화된 계정에 대한 재활성화 시도 예외
 *
 * <p>
 * 이미 활성 상태인 계정에 대해 재활성화를 시도할 때 발생하는 예외입니다.
 * </p>
 *
 * @author Claude Code
 * @since 1.0
 */
public class AccountAlreadyActivatedException extends RuntimeException {

  /**
   * 메시지를 포함한 예외를 생성합니다.
   *
   * @param message 예외 메시지
   */
  public AccountAlreadyActivatedException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인을 포함한 예외를 생성합니다.
   *
   * @param message 예외 메시지
   * @param cause   예외 원인
   */
  public AccountAlreadyActivatedException(String message, Throwable cause) {
    super(message, cause);
  }
}