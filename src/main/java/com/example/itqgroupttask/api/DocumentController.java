package com.example.itqgroupttask.api;

import com.example.itqgroupttask.api.dto.*;
import com.example.itqgroupttask.service.ConcurrentApproveTester;
import com.example.itqgroupttask.service.DocumentMapper;
import com.example.itqgroupttask.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final ConcurrentApproveTester concurrentApproveTester;

    @PostMapping
    public DocumentDto create(@RequestBody @Valid CreateDocumentRequest request) {
        var doc = documentService.create(request.getInitiator(), request.getAuthor(), request.getTitle());
        return DocumentMapper.toDto(doc, null);
    }

    @GetMapping("/{id}")
    public DocumentDto getOne(@PathVariable long id,
                              @RequestParam(defaultValue = "true") boolean includeHistory) {
        var doc = documentService.getOrThrow(id);
        var history = includeHistory ? documentService.getHistory(doc) : null;
        return DocumentMapper.toDto(doc, history);
    }

    @GetMapping
    public ResponseEntity<?> getMany(@RequestParam(required = false) List<Long> ids,
                                     @PageableDefault(size = 20) Pageable pageable,
                                     @RequestParam(defaultValue = "false") boolean includeHistory) {
        if (ids != null && !ids.isEmpty()) {
            var docs = documentService.listByIds(ids);
            // для пакетного получения пагинацию делаем на уровне входного списка ids:
            // это упрощение; для прод-сценария лучше отдельный endpoint с курсорами.
            int from = Math.min((int) pageable.getOffset(), docs.size());
            int to = Math.min(from + pageable.getPageSize(), docs.size());
            var pageSlice = docs.subList(from, to);
            var mapped = pageSlice.stream()
                    .map(d -> DocumentMapper.toDto(d, includeHistory ? documentService.getHistory(d) : null))
                    .toList();
            return ResponseEntity.ok(mapped);
        }

        Page<DocumentDto> page = documentService.list(pageable)
                .map(d -> DocumentMapper.toDto(d, includeHistory ? documentService.getHistory(d) : null));
        return ResponseEntity.ok(page);
    }

    @PostMapping("/submit")
    public BatchOperationResponse submit(@RequestBody @Valid BatchIdsRequest request) {
        var results = documentService.submitBatch(request.getInitiator(), request.getIds(), request.getComment());
        return BatchOperationResponse.builder().results(results).build();
    }

    @PostMapping("/approve")
    public BatchOperationResponse approve(@RequestBody @Valid BatchIdsRequest request) {
        var results = documentService.approveBatch(request.getInitiator(), request.getIds(), request.getComment());
        return BatchOperationResponse.builder().results(results).build();
    }

    @GetMapping("/search")
    public Page<DocumentDto> search(@RequestParam(required = false) com.example.itqgroupttask.domain.DocumentStatus status,
                                   @RequestParam(required = false) String author,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                   Pageable pageable) {
        return documentService.search(Optional.ofNullable(status), Optional.ofNullable(author), from, to, pageable)
                .map(d -> DocumentMapper.toDto(d, null));
    }

    @PostMapping("/test-concurrent-approve")
    public ConcurrentApproveTestResponse testConcurrentApprove(@RequestBody @Valid ConcurrentApproveTestRequest request) {
        return concurrentApproveTester.run(
                request.getInitiator(),
                request.getDocumentId(),
                request.getThreads(),
                request.getAttempts()
        );
    }
}

