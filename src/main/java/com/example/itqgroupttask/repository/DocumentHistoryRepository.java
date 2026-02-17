package com.example.itqgroupttask.repository;

import com.example.itqgroupttask.domain.Document;
import com.example.itqgroupttask.domain.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

    List<DocumentHistory> findByDocumentOrderByCreatedAtAsc(Document document);

}

