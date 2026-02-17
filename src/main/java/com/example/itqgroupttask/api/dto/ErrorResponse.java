package com.example.itqgroupttask.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    String code;
    String message;
}

