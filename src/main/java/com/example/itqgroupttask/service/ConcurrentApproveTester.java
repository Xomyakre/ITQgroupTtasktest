package com.example.itqgroupttask.service;

import com.example.itqgroupttask.api.dto.BatchItemResultDto;
import com.example.itqgroupttask.api.dto.ConcurrentApproveTestResponse;
import com.example.itqgroupttask.domain.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrentApproveTester {

    private final DocumentService documentService;

    public ConcurrentApproveTestResponse run(String initiator, long documentId, int threads, int attempts) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<BatchItemResultDto>> tasks = new ArrayList<>(attempts);
            for (int i = 0; i < attempts; i++) {
                tasks.add(() -> {
                    var res = documentService.approveBatch(initiator, List.of(documentId), "concurrent-test");
                    if (res == null || res.isEmpty()) {
                        return BatchItemResultDto.builder()
                                .id(documentId)
                                .result("ERROR")
                                .message("Empty approve result")
                                .build();
                    }
                    return res.get(0);
                });
            }

            var success = new LongAdder();
            var conflict = new LongAdder();
            var notFound = new LongAdder();
            var registryError = new LongAdder();
            var otherError = new LongAdder();

            List<Future<BatchItemResultDto>> futures = pool.invokeAll(tasks);
            for (Future<BatchItemResultDto> f : futures) {
                BatchItemResultDto r;
                try {
                    r = f.get(60, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    otherError.increment();
                    continue;
                }
                switch (r.getResult()) {
                    case "SUCCESS" -> success.increment();
                    case "CONFLICT" -> conflict.increment();
                    case "NOT_FOUND" -> notFound.increment();
                    case "REGISTRY_ERROR" -> registryError.increment();
                    default -> otherError.increment();
                }
            }

            DocumentStatus finalStatus;
            try {
                finalStatus = documentService.getOrThrow(documentId).getStatus();
            } catch (Exception ex) {
                finalStatus = null;
            }

            return ConcurrentApproveTestResponse.builder()
                    .success(success.sum())
                    .conflict(conflict.sum())
                    .notFound(notFound.sum())
                    .registryError(registryError.sum())
                    .otherError(otherError.sum())
                    .finalStatus(finalStatus)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ConcurrentApproveTestResponse.builder()
                    .success(0)
                    .conflict(0)
                    .notFound(0)
                    .registryError(0)
                    .otherError(attempts)
                    .finalStatus(null)
                    .build();
        } finally {
            pool.shutdownNow();
        }
    }
}

