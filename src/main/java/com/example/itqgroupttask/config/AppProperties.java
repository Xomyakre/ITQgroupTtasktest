package com.example.itqgroupttask.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private int batchSize = 100;
    private Workers workers = new Workers();

    @Data
    public static class Workers {
        private boolean enabled = true;
        private long submitFixedDelayMs = 5000;
        private long approveFixedDelayMs = 5000;
    }
}

