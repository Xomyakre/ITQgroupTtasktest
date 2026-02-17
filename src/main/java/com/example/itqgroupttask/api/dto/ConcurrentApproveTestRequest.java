package com.example.itqgroupttask.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConcurrentApproveTestRequest {

    @NotBlank
    private String initiator;

    @NotNull
    private Long documentId;

    @Min(1)
    @Max(128)
    private int threads = 8;

    @Min(1)
    @Max(10_000)
    private int attempts = 100;
}

