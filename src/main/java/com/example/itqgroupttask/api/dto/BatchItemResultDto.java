package com.example.itqgroupttask.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BatchItemResultDto {
    Long id;
    String result; // SUCCESS | CONFLICT | NOT_FOUND | REGISTRY_ERROR | ERROR
    String message;
}

