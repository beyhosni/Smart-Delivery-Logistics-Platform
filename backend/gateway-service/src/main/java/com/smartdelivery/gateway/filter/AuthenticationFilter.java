package com.smartdelivery.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter implements GatewayFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/fallback/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for excluded paths
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        if (!request.getHeaders().containsKey(AUTHORIZATION_HEADER)) {
            return handleUnauthorized(exchange);
        }

        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return handleUnauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Here you would validate the JWT token
        // For now, we'll just check if the token exists
        if (token.isEmpty()) {
            return handleUnauthorized(exchange);
        }

        // Add user information to headers for downstream services
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", extractUserIdFromToken(token))
                .header("X-User-Role", extractUserRoleFromToken(token))
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private String extractUserIdFromToken(String token) {
        // In a real implementation, you would decode the JWT and extract the user ID
        return "user123"; // Placeholder
    }

    private String extractUserRoleFromToken(String token) {
        // In a real implementation, you would decode the JWT and extract the user role
        return "USER"; // Placeholder
    }
}
