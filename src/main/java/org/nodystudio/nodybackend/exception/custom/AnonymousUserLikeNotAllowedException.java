package org.nodystudio.nodybackend.exception.custom;

import org.nodystudio.nodybackend.dto.code.ErrorCode;

/**
 * 익명 사용자 좋아요 시도 예외
 * 
 * <p>
 * 익명 사용자가 좋아요를 누르려고 할 때 발생하는 예외입니다.
 * 좋아요 기능은 인증된 사용자만 사용할 수 있습니다.
 * </p>
 */
public class AnonymousUserLikeNotAllowedException extends BusinessException {

  /**
   * 익명 사용자 좋아요 시도 예외를 생성합니다.
   *
   * @param message 예외 메시지
   */
  public AnonymousUserLikeNotAllowedException(String message) {
    super(message, ErrorCode.ANONYMOUS_USER_LIKE_NOT_ALLOWED);
  }

  /**
   * 기본 메시지로 익명 사용자 좋아요 시도 예외를 생성합니다.
   */
  public AnonymousUserLikeNotAllowedException() {
    super(ErrorCode.ANONYMOUS_USER_LIKE_NOT_ALLOWED);
  }
}