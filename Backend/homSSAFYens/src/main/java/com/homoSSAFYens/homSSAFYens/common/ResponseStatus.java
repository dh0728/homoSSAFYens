package com.homoSSAFYens.homSSAFYens.common;

import lombok.Getter;

 @Getter
public enum ResponseStatus {
    SUCCESS("success"),
    BAD_REQUEST("bad_request"),
    UNAUTHORIZED("unauthorized"),
    FORBIDDEN("forbidden"),
    NOT_FOUND("not_found"),
    INTERNAL_SERVER_ERROR("internal_server_error");

    private final String value;

    ResponseStatus(String value) {
        this.value = value;
    }
}
