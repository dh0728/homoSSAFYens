package com.homoSSAFYens.homSSAFYens.exception;

import com.homoSSAFYens.homSSAFYens.common.ApiResponse;
import com.homoSSAFYens.homSSAFYens.common.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 요청 파라미터의 타입이 일치하지 않을 때 400 Bad Request 처리용
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("요청 파라미터 '%s'의 타입이 잘못되었습니다. (입력값: '%s')", e.getName(), e.getValue());
        log.warn("Bad Request: {}", message);
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.BAD_REQUEST, message);
        return new ResponseEntity<>(response, ResponseCode.BAD_REQUEST.getHttpStatus());
    }

    // uri 존재 안할 때 404 처리용
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFoundException(NoResourceFoundException e) {
        log.warn("404 Not Found: URL = {}, Message = {}", e.getResourcePath(), e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.NOT_FOUND, "요청하신 API를 찾을 수 없습니다.");
        return new ResponseEntity<>(response, ResponseCode.NOT_FOUND.getHttpStatus());
    }

    // 서버 내부 오류 처리용
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("Internal Server Error", e);
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.INTERNAL_ERROR, "서버에 예상치 못한 오류가 발생했습니다. 관리자에게 문의해주세요.");
        return new ResponseEntity<>(response, ResponseCode.INTERNAL_ERROR.getHttpStatus());
    }
}