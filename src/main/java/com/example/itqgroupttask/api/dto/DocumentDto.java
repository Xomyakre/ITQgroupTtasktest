package com.example.itqgroupttask.api.dto;

import com.example.itqgroupttask.domain.DocumentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class DocumentDto {
    Long id;
    String number;
    String author;
    String title;
    DocumentStatus status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    Long version;
    List<DocumentHistoryDto> history;
}

