package org.nodystudio.nodybackend.exception.custom;

import org.nodystudio.nodybackend.dto.code.ErrorCode;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends BusinessException {

    /**
     * 사용자를 찾을 수 없는 예외 생성자 (기본 메시지 사용)
     */
    public UserNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 사용자를 찾을 수 없는 예외 생성자 (커스텀 메시지 사용)
     *
     * @param message 예외 메시지
     */
    public UserNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 사용자를 찾을 수 없는 예외 생성자 (커스텀 메시지 및 원인 포함)
     *
     * @param message 예외 메시지
     * @param cause   원인 예외
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND, cause);
    }

    /**
     * 특정 사용자 ID로 사용자를 찾을 수 없는 경우의 예외 생성
     *
     * @param userId 사용자 ID
     * @return UserNotFoundException 인스턴스
     */
    public static UserNotFoundException byUserId(String userId) {
        String message = String.format("사용자 ID '%s'로 사용자를 찾을 수 없습니다.", userId);
        return new UserNotFoundException(message);
    }

    /**
     * 특정 이메일로 사용자를 찾을 수 없는 경우의 예외 생성
     *
     * @param email 이메일
     * @return UserNotFoundException 인스턴스
     */
    public static UserNotFoundException byEmail(String email) {
        String message = String.format("이메일 '%s'로 사용자를 찾을 수 없습니다.", email);
        return new UserNotFoundException(message);
    }
}