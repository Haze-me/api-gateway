package com.marketplace.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Placeholder for programmatic route configuration.
 * Our routes are defined in application.yml which is cleaner and
 * easier to read. This class exists for future custom route logic
 * (e.g. rate limiting, load balancing between instances).
 */
@Configuration
public class GatewayConfig {
    // Routes are configured in application.yml via spring.cloud.gateway.routes
    // Add programmatic routes here in the future if needed
}