package org.nodystudio.nodybackend.exception.custom;

import org.nodystudio.nodybackend.dto.code.ErrorCode;

/**
 * 이미 탈퇴한 계정 예외
 * 
 * 이미 탈퇴한 계정에 대해 다시 탈퇴를 시도하거나,
 * 탈퇴한 계정으로 특정 작업을 수행하려 할 때 발생합니다.
 */
public class AccountAlreadyDeactivatedException extends BusinessException {

    private final String userId;

    public AccountAlreadyDeactivatedException(String userId) {
        super(String.format("사용자 ID '%s'는 이미 탈퇴한 계정입니다.", userId),
              ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        this.userId = userId;
    }

    public AccountAlreadyDeactivatedException(String userId, String message) {
        super(message, ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}