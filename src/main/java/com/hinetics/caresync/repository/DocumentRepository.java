package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Document;
import com.hinetics.caresync.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findByDocumentType(DocumentType documentType);

}
