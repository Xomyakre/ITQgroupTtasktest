package com.example.itqgroupttask.repository;

import com.example.itqgroupttask.domain.Document;
import com.example.itqgroupttask.domain.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    List<Document> findByIdIn(List<Long> ids);

    @Query("select d from Document d where d.status = :status order by d.id asc")
    List<Document> findNextBatchByStatus(@Param("status") DocumentStatus status,
                                         org.springframework.data.domain.Pageable pageable);

    @Query("select d from Document d where d.id in :ids")
    List<Document> findAllByIds(@Param("ids") Collection<Long> ids);
}

