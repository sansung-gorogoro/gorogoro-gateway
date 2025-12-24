package com.gorogoro.gateway.exception;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<FieldError> errors
) {
    // 상세 에러 정보를 담는 내부 레코드
    public record FieldError(
            String field,
            String reason,
            Object rejectedValue
    ) {
    }

    // 1. 에러 목록이 있는 경우 (주로 @Valid 실패 시 사용)
    public static ErrorResponse of(String code, String message, List<FieldError> errors) {
        return new ErrorResponse(code, message, errors);
    }

    // 2. 에러 목록이 없는 경우 (일반 비즈니스 예외)
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
}
