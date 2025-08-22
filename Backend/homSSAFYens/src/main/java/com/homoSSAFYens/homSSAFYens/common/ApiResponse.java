package com.homoSSAFYens.homSSAFYens.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

 @Getter @AllArgsConstructor
public class ApiResponse<T> {
    private String status; // success, bad_request...
    private int code;      // 숫자 코드
    private String message;// 요청 처리 결과 설명
    private T data;        // 실제 데이터

    public static <T> ApiResponse<T> of(ResponseCode responseCode, String message, T data) {
        return new ApiResponse<>(
                responseCode.getStatus().getValue(),
                responseCode.getCode(),
                message != null ? message : responseCode.getDefaultMessage(),
                data
        );
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return of(ResponseCode.SUCCESS, message, data);
    }

    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message) {
        return of(responseCode, message, null);
    }
}
