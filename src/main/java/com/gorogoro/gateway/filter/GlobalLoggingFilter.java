package com.gorogoro.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-1)
public class GlobalLoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        // 요청 로깅
        log.info("╔════════════════════════════════════════════════════════════════");
        log.info("║ 📨 REQUEST");
        log.info("╠════════════════════════════════════════════════════════════════");
        log.info("║ Method: {}", exchange.getRequest().getMethod());
        log.info("║ URI: {}", exchange.getRequest().getURI());
        log.info("║ Path: {}", exchange.getRequest().getPath());
        log.info("║ Remote Address: {}", exchange.getRequest().getRemoteAddress());
        log.info("║ Headers:");
        exchange.getRequest().getHeaders().forEach((name, values) -> {
            log.info("║   {}: {}", name, String.join(", ", values));
        });
        log.info("╚════════════════════════════════════════════════════════════════");

        // 응답 처리 및 로깅
        return chain.filter(exchange).doFinally(signal -> {
            long duration = System.currentTimeMillis() - startTime;

            log.info("╔════════════════════════════════════════════════════════════════");
            log.info("║ 📤 RESPONSE");
            log.info("╠════════════════════════════════════════════════════════════════");
            log.info("║ Status: {}", exchange.getResponse().getStatusCode());
            log.info("║ Duration: {}ms", duration);
            log.info("║ Signal: {}", signal);
            log.info("║ Headers:");
            exchange.getResponse().getHeaders().forEach((name, values) -> {
                log.info("║   {}: {}", name, String.join(", ", values));
            });
            log.info("╚════════════════════════════════════════════════════════════════");
        });
    }
}

