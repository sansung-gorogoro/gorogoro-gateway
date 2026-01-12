package com.gorogoro.gateway.exception;

import com.gorogoro.gateway.exception.code.GatewayErrorCode;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class CustomGlobalExceptionHandler extends AbstractErrorWebExceptionHandler {
    public CustomGlobalExceptionHandler(ErrorAttributes errorAttributes,
                                        WebProperties webProperties,
                                        ApplicationContext applicationContext,
                                        ServerCodecConfigurer configurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
        this.setMessageReaders(configurer.getReaders());
    }

    // 모든 에러 요청을 renderErrorResponse 메서드로 연결
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    // 에러 응답 생성 로직
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        // 발생한 실제 예외 객체 가져오기
        Throwable throwable = getError(request);

        // 예외 종류에 따른 에러 코드 분석
        ErrorDetails errorDetails = analyzeException(throwable);

        // 응답 바디 재구성
        ErrorResponse errorResponse = ErrorResponse.of(errorDetails.code, errorDetails.message);

        // JSON 응답 반환
        return ServerResponse.status(errorDetails.status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorResponse));
    }

    private ErrorDetails analyzeException(Throwable error) {
        // 사용자 정의 예외 처리
        if (error instanceof BaseException ex) {
            ErrorCode errorCode = ex.getErrorCode();
            return new ErrorDetails(
                    errorCode.getHttpStatus(),
                    errorCode.getCode(),
                    errorCode.getMessage()
            );
        } else if (error instanceof ResponseStatusException ex) {
            ErrorCode errorCode = mapResponseStatusException(ex);

            return new ErrorDetails(
                    errorCode.getHttpStatus(),
                    errorCode.getCode(),
                    errorCode.getMessage()
            );
        } else {
            ErrorCode errorCode = GatewayErrorCode.INTERNAL_SERVER_ERROR;
            return new ErrorDetails(
                    errorCode.getHttpStatus(),
                    errorCode.getCode(),
                    errorCode.getMessage()
            );
        }
    }

    private ErrorCode mapResponseStatusException(ResponseStatusException ex) {
        return switch (ex.getStatusCode().value()) {
            case 404 -> GatewayErrorCode.NOT_FOUND;
            case 405 -> GatewayErrorCode.METHOD_NOT_ALLOWED;
            case 400 -> GatewayErrorCode.BAD_REQUEST;
            default -> GatewayErrorCode.INTERNAL_SERVER_ERROR;
        };
    }

    private record ErrorDetails(HttpStatusCode status, String code, String message) {}
}
