package com.example.itqgroupttask.service;

import com.example.itqgroupttask.api.dto.DocumentDto;
import com.example.itqgroupttask.api.dto.DocumentHistoryDto;
import com.example.itqgroupttask.domain.Document;
import com.example.itqgroupttask.domain.DocumentHistory;

import java.util.List;

public final class DocumentMapper {
    private DocumentMapper() {
    }

    public static DocumentDto toDto(Document doc, List<DocumentHistory> history) {
        return DocumentDto.builder()
                .id(doc.getId())
                .number(doc.getNumber())
                .author(doc.getAuthor())
                .title(doc.getTitle())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .version(doc.getVersion())
                .history(history == null ? null : history.stream().map(DocumentMapper::toHistoryDto).toList())
                .build();
    }

    public static DocumentHistoryDto toHistoryDto(DocumentHistory h) {
        return DocumentHistoryDto.builder()
                .id(h.getId())
                .actor(h.getActor())
                .action(h.getAction())
                .comment(h.getComment())
                .createdAt(h.getCreatedAt())
                .build();
    }
}

