package com.example.itqgroupttask.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchIdsRequest {

    @NotBlank
    private String initiator;

    @NotEmpty
    @Size(min = 1, max = 1000)
    private List<Long> ids;

    private String comment;
}

