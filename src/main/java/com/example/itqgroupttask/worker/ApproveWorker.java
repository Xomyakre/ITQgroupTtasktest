package com.example.itqgroupttask.worker;

import com.example.itqgroupttask.config.AppProperties;
import com.example.itqgroupttask.domain.DocumentStatus;
import com.example.itqgroupttask.repository.DocumentRepository;
import com.example.itqgroupttask.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApproveWorker {

    private final AppProperties props;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Scheduled(fixedDelayString = "${app.workers.approve-fixed-delay-ms:5000}")
    public void run() {
        if (!props.getWorkers().isEnabled()) {
            return;
        }

        int batchSize = Math.max(1, props.getBatchSize());
        var docs = documentRepository.findNextBatchByStatus(DocumentStatus.SUBMITTED, PageRequest.of(0, batchSize));
        if (docs.isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();
        var ids = docs.stream().map(d -> d.getId()).toList();
        var results = documentService.approveBatch("APPROVE-worker", ids, null);
        long ok = results.stream().filter(r -> "SUCCESS".equals(r.getResult())).count();
        log.info("APPROVE-worker processed {} docs (success={}) in {} ms", ids.size(), ok, System.currentTimeMillis() - start);
    }
}

