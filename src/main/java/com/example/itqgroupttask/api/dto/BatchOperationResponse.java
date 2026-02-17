package com.example.itqgroupttask.api.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BatchOperationResponse {
    List<BatchItemResultDto> results;
}

