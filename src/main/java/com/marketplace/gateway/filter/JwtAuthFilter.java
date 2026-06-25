package com.marketplace.gateway.filter;

import com.marketplace.gateway.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.marketplace.gateway.config.MarketplaceGatewayProperties;

@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtService jwtService;
    private final MarketplaceGatewayProperties gatewayProperties;

    public JwtAuthFilter(JwtService jwtService, MarketplaceGatewayProperties gatewayProperties) {
        super(Config.class);
        this.jwtService = jwtService;
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("[Gateway] Incoming: {} {}", request.getMethod(), path);

            // Step 1: Block internal endpoints
            if (isBlocked(path)) {
                log.warn("[Gateway] Blocked internal path: {}", path);
                return reject(exchange, HttpStatus.FORBIDDEN,
                        "{\"success\":false,\"message\":\"Access denied.\"}");
            }

            // Step 2: Public paths — no token needed
            if (isPublic(path)) {
                log.debug("[Gateway] Public path — forwarding: {}", path);
                return chain.filter(exchange);
            }

            // Step 3: Check Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("[Gateway] Missing token on: {}", path);
                return reject(exchange, HttpStatus.UNAUTHORIZED,
                        "{\"success\":false,\"message\":\"Authorization token is required.\"}");
            }

            String token = authHeader.substring(7);

            // Step 4: Validate token
            if (!jwtService.isValid(token)) {
                log.debug("[Gateway] Invalid token on: {}", path);
                return reject(exchange, HttpStatus.UNAUTHORIZED,
                        "{\"success\":false,\"message\":\"Invalid or expired token.\"}");
            }

            // Step 5: Forward with user info headers
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User", username != null ? username : "")
                    .header("X-Auth-Role", role != null ? role : "")
                    .build();

            log.debug("[Gateway] Auth OK — user: {} role: {} → {}", username, role, path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isPublic(String path) {
        return gatewayProperties.getPublicPaths().stream().anyMatch(path::startsWith);
    }

    private boolean isBlocked(String path) {
        return gatewayProperties.getBlockedPaths().stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {}
}