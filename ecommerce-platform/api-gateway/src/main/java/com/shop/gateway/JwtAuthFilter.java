package com.shop.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Single place where authentication happens. Validates the JWT and forwards the
 * caller's identity to downstream services as X-User-* headers.
 * Identity headers sent by the outside world are always stripped first.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        ServerHttpRequest.Builder builder = request.mutate().headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Email");
            h.remove("X-User-Role");
        });

        boolean isPublic = path.startsWith("/api/auth/")
                || (HttpMethod.GET.equals(method) && path.startsWith("/api/products"));

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(auth.substring(7)).getPayload();
                String role = claims.get("role", String.class);
                boolean writesProducts = path.startsWith("/api/products") && !HttpMethod.GET.equals(method);
                if (writesProducts && !"ADMIN".equals(role)) {
                    return reject(exchange, HttpStatus.FORBIDDEN);
                }
                builder.header("X-User-Id", claims.getSubject())
                       .header("X-User-Email", String.valueOf(claims.get("email", String.class)))
                       .header("X-User-Role", String.valueOf(role));
                return chain.filter(exchange.mutate().request(builder.build()).build());
            } catch (JwtException | IllegalArgumentException e) {
                return reject(exchange, HttpStatus.UNAUTHORIZED);
            }
        }
        if (isPublic) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }
        return reject(exchange, HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
