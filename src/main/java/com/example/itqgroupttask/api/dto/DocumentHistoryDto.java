package com.example.itqgroupttask.api.dto;

import com.example.itqgroupttask.domain.DocumentAction;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class DocumentHistoryDto {
    Long id;
    String actor;
    DocumentAction action;
    String comment;
    OffsetDateTime createdAt;
}

