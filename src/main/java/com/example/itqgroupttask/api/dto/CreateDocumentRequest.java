package com.example.itqgroupttask.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @NotBlank
    private String initiator;

    @NotBlank
    private String author;

    @NotBlank
    private String title;
}

