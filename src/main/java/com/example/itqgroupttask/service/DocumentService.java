package com.example.itqgroupttask.service;

import com.example.itqgroupttask.api.dto.BatchItemResultDto;
import com.example.itqgroupttask.domain.*;
import com.example.itqgroupttask.exception.ConflictException;
import com.example.itqgroupttask.exception.NotFoundException;
import com.example.itqgroupttask.exception.RegistryException;
import com.example.itqgroupttask.repository.ApprovalRegistryRepository;
import com.example.itqgroupttask.repository.DocumentHistoryRepository;
import com.example.itqgroupttask.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository historyRepository;
    private final ApprovalRegistryRepository approvalRegistryRepository;

    @Transactional
    public Document create(String initiator, String author, String title) {
        var doc = Document.builder()
                .number(DocumentNumberGenerator.nextNumber())
                .author(author)
                .title(title)
                .status(DocumentStatus.DRAFT)
                .build();
        try {
            return documentRepository.save(doc);
        } catch (DataIntegrityViolationException ex) {
            doc.setNumber(DocumentNumberGenerator.nextNumber());
            return documentRepository.save(doc);
        }
    }

    @Transactional(readOnly = true)
    public Document getOrThrow(long id) {
        return documentRepository.findById(id).orElseThrow(() -> new NotFoundException("Document not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DocumentHistory> getHistory(Document doc) {
        return historyRepository.findByDocumentOrderByCreatedAtAsc(doc);
    }

    @Transactional(readOnly = true)
    public Page<Document> list(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Document> listByIds(List<Long> ids) {
        return documentRepository.findAllByIds(ids);
    }

    @Transactional(readOnly = true)
    public Page<Document> search(Optional<DocumentStatus> status,
                                Optional<String> author,
                                OffsetDateTime from,
                                OffsetDateTime to,
                                Pageable pageable) {
        Specification<Document> spec = where(createdBetween(from, to))
                .and(status.map(DocumentService::hasStatus).orElse(null))
                .and(author.filter(a -> !a.isBlank()).map(DocumentService::authorLike).orElse(null));
        return documentRepository.findAll(spec, pageable);
    }

    @Transactional
    public List<BatchItemResultDto> submitBatch(String initiator, List<Long> ids, String comment) {
        var results = new ArrayList<BatchItemResultDto>(ids.size());
        for (Long id : ids) {
            results.add(submitOneResult(initiator, id, comment));
        }
        return results;
    }

    @Transactional
    public List<BatchItemResultDto> approveBatch(String initiator, List<Long> ids, String comment) {
        var results = new ArrayList<BatchItemResultDto>(ids.size());
        for (Long id : ids) {
            results.add(approveOneResult(initiator, id, comment));
        }
        return results;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void submitOne(String initiator, long id, String comment) {
        var doc = getOrThrow(id);
        if (doc.getStatus() != DocumentStatus.DRAFT) {
            throw new ConflictException("Invalid status transition: " + doc.getStatus() + " -> SUBMITTED");
        }
        doc.setStatus(DocumentStatus.SUBMITTED);
        documentRepository.save(doc);
        historyRepository.save(DocumentHistory.builder()
                .document(doc)
                .actor(initiator)
                .action(DocumentAction.SUBMIT)
                .comment(comment)
                .createdAt(OffsetDateTime.now())
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approveOne(String initiator, long id, String comment) {
        var doc = getOrThrow(id);
        if (doc.getStatus() != DocumentStatus.SUBMITTED) {
            throw new ConflictException("Invalid status transition: " + doc.getStatus() + " -> APPROVED");
        }

        doc.setStatus(DocumentStatus.APPROVED);
        documentRepository.save(doc);

        historyRepository.save(DocumentHistory.builder()
                .document(doc)
                .actor(initiator)
                .action(DocumentAction.APPROVE)
                .comment(comment)
                .createdAt(OffsetDateTime.now())
                .build());

        try {
            approvalRegistryRepository.save(ApprovalRegistry.builder()
                    .document(doc)
                    .approvedBy(initiator)
                    .approvedAt(OffsetDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new RegistryException("Failed to register approval for document: " + id);
        }
    }

    private BatchItemResultDto submitOneResult(String initiator, Long id, String comment) {
        try {
            submitOne(initiator, id, comment);
            return BatchItemResultDto.builder().id(id).result("SUCCESS").build();
        } catch (NotFoundException ex) {
            return BatchItemResultDto.builder().id(id).result("NOT_FOUND").message(ex.getMessage()).build();
        } catch (ConflictException ex) {
            return BatchItemResultDto.builder().id(id).result("CONFLICT").message(ex.getMessage()).build();
        } catch (Exception ex) {
            return BatchItemResultDto.builder().id(id).result("ERROR").message(ex.getMessage()).build();
        }
    }

    private BatchItemResultDto approveOneResult(String initiator, Long id, String comment) {
        try {
            approveOne(initiator, id, comment);
            return BatchItemResultDto.builder().id(id).result("SUCCESS").build();
        } catch (NotFoundException ex) {
            return BatchItemResultDto.builder().id(id).result("NOT_FOUND").message(ex.getMessage()).build();
        } catch (ConflictException ex) {
            return BatchItemResultDto.builder().id(id).result("CONFLICT").message(ex.getMessage()).build();
        } catch (RegistryException ex) {
            return BatchItemResultDto.builder().id(id).result("REGISTRY_ERROR").message(ex.getMessage()).build();
        } catch (Exception ex) {
            return BatchItemResultDto.builder().id(id).result("ERROR").message(ex.getMessage()).build();
        }
    }

    private static Specification<Document> hasStatus(DocumentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Document> authorLike(String author) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    private static Specification<Document> createdBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }
}

