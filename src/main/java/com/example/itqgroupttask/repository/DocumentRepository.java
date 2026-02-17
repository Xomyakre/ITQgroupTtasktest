package com.example.itqgroupttask.repository;

import com.example.itqgroupttask.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByIdIn(List<Long> ids);

}

