package com.gorogoro.gateway.filter;

import com.gorogoro.gateway.exception.BaseException;
import com.gorogoro.gateway.exception.code.GatewayErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private static final String ROLE_KEY = "role";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private final PublicKey publicKey;

    public AuthorizationHeaderFilter(
            @Value("classpath:public_key.pem") Resource publicKeyResource) {
        super(Config.class);
        this.publicKey = loadPublicKey(publicKeyResource);
    }

    public static class Config {
    }

    private PublicKey loadPublicKey(Resource resource) {
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String keyData = reader.lines()
                    .filter(line -> !line.startsWith("-----BEGIN") && !line.startsWith("-----END"))
                    .collect(Collectors.joining());

            byte[] decodedKey = Base64.getDecoder().decode(keyData);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new BaseException(GatewayErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 요청 정보 추출
            ServerHttpRequest serverHttpRequest = exchange.getRequest();

            String authHeader = serverHttpRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null) {
                throw new BaseException(GatewayErrorCode.EMPTY_TOKEN);
            }

            if (!authHeader.startsWith(BEARER_PREFIX)) {
                throw new BaseException(GatewayErrorCode.INVALID_TOKEN);
            }

            // 토큰 추출
            String token = authHeader.substring(BEARER_PREFIX.length());

            try {
                // 공개 키를 사용하여 토큰이 변조 여부 검증 및 파싱
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String role = claims.get(ROLE_KEY, String.class);

                ServerHttpRequest newRequest = serverHttpRequest.mutate()
                        .headers(header -> {
                            header.remove(HttpHeaders.AUTHORIZATION);
                            header.set(HEADER_USER_ID, userId);
                            header.set(HEADER_USER_ROLE, role);
                        })
                        .build();

                return chain.filter(exchange.mutate().request(newRequest).build());
            } catch (ExpiredJwtException e) {
                throw new BaseException(GatewayErrorCode.TOKEN_EXPIRED, e);
            } catch (Exception e) {
                throw new BaseException(GatewayErrorCode.UNAUTHORIZED_ACCESS, e);
            }
        };
    }
}
