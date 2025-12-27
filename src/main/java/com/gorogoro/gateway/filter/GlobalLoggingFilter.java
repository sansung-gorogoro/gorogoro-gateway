package com.gorogoro.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        // Gateway가 생성한 고유 Request ID (로그 추적의 핵심)
        String requestId = request.getId();

        // [요청 로그] ID, Method, URI (헤더 제외: 보안 및 가독성)
        log.info("[REQ] [{}] {} {}", requestId, request.getMethod(), request.getURI());

        // 비동기 처리 후 응답 로그 출력
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();

            // [응답 로그] ID, Status, 소요 시간
            log.info("[RES] [{}] Status: {} ({}ms)", requestId, response.getStatusCode(), duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

