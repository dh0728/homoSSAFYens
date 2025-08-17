package com.homoSSAFYens.homSSAFYens.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

 @Getter
public enum ResponseCode {
    SUCCESS(ResponseStatus.SUCCESS, HttpStatus.OK, 200, "요청이 성공적으로 처리되었습니다."),
    BAD_REQUEST(ResponseStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST, 400, "잘못된 요청입니다."),
    UNAUTHORIZED(ResponseStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    FORBIDDEN(ResponseStatus.FORBIDDEN, HttpStatus.FORBIDDEN, 403, "접근이 거부되었습니다."),
    NOT_FOUND(ResponseStatus.NOT_FOUND, HttpStatus.NOT_FOUND, 404, "데이터를 찾을 수 없습니다."),
    INTERNAL_ERROR(ResponseStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다.");

    private final ResponseStatus status; // success, bad_request...
    private final HttpStatus httpStatus; // Spring HttpStatus
    private final int code;              // 200, 400...
    private final String defaultMessage; // 기본 메시지

    ResponseCode(ResponseStatus status, HttpStatus httpStatus, int code, String defaultMessage) {
        this.status = status;
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
