package com.example.itqgroupttask.generator;

import lombok.Data;

@Data
public class GeneratorConfig {
    private String baseUrl = "http://localhost:8080";
    private int n = 100;
    private String initiator = "generator";
    private String author = "author";
    private String titlePrefix = "Generated document ";
}

