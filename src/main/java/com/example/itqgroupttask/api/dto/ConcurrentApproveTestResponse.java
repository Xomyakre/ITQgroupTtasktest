package com.example.itqgroupttask.api.dto;

import com.example.itqgroupttask.domain.DocumentStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConcurrentApproveTestResponse {
    long success;
    long conflict;
    long notFound;
    long registryError;
    long otherError;
    DocumentStatus finalStatus;
}

