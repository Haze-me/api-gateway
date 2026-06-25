package com.marketplace.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway")
public class MarketplaceGatewayProperties {
    private List<String> publicPaths;
    private List<String> blockedPaths;
}