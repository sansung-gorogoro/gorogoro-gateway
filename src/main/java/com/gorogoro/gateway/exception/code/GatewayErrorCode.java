package com.gorogoro.gateway.exception.code;

import com.gorogoro.gateway.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GatewayErrorCode implements ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "GTW-0001", "유효하지 않은 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "GTW-0002", "유효하지 않은 토큰입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "GTW-0003", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GTW-0004", "서버 내부 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "GTW-0005", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "GTW-0006", "존재하지 않는 경로입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "GTW-0007", "허용되지 않은 HTTP 메서드입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
