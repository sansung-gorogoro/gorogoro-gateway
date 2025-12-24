package com.gorogoro.gateway.filter;

import com.gorogoro.gateway.exception.BaseException;
import com.gorogoro.gateway.exception.code.GatewayErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private static final String ALGORITHM = "RSA";
    private static final String ROLE_KEY = "role";
    private static final String UNKNOWN_ROLE = "UNKNOWN";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String KEY_FOOTER = "-----END PUBLIC KEY-----";

    private final Resource publicKeyResource;
    private PublicKey publicKey;

    public AuthorizationHeaderFilter(ResourceLoader resourceLoader, @Value("${jwt.public-key-path}") String publicKeyPath) {
        super(Config.class);
        this.publicKeyResource = resourceLoader.getResource(publicKeyPath);
    }

    public static class Config {
    }

    @PostConstruct
    private void loadPublicKey() {
        try {
            // 파일 읽기
            String publicKeyString = StreamUtils.copyToString(publicKeyResource.getInputStream(), StandardCharsets.UTF_8);

            // 헤더, 푸터, 공백 제거
            String publicKey = publicKeyString
                    .replace(KEY_HEADER, "")
                    .replace(KEY_FOOTER, "")
                    .replaceAll("\\s", "");

            // Base64 디코딩 및 PublicKey 객체 생성
            byte[] encoded = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            this.publicKey = KeyFactory.getInstance(ALGORITHM).generatePublic(keySpec);

            System.out.println("공개키 로딩 완료");
        } catch (Exception e) {
            throw new RuntimeException("공개키를 로드하는 데 실패했습니다.", e);
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
                        .verifyWith(getPublicKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                String role = claims.get(ROLE_KEY, String.class) != null
                        ? claims.get(ROLE_KEY, String.class)
                        : UNKNOWN_ROLE;

                ServerHttpRequest newRequest = serverHttpRequest.mutate()
                        .headers(header -> {
                            header.remove(HttpHeaders.AUTHORIZATION);
                            header.add(HEADER_USER_ID, userId);
                            header.add(HEADER_USER_ROLE, role);
                        })
                        .build();

                return chain.filter(exchange.mutate().request(newRequest).build());
            } catch (Exception e) {
                throw new BaseException(GatewayErrorCode.UNAUTHORIZED_ACCESS);
            }
        };
    }

    private PublicKey getPublicKey() {
        if (this.publicKey == null) {
            throw new RuntimeException("공개키가 초기화되지 않았습니다.");
        }

        return this.publicKey;
    }
}
