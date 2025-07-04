package org.nodystudio.nodybackend.exception.custom;

import org.nodystudio.nodybackend.dto.code.ErrorCode;

public class InvalidCoordinateException extends BusinessException {
    public InvalidCoordinateException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
