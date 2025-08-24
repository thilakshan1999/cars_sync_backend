package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Document;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findByUser(User user);
    List<Document> findByUserAndDocumentType(User user, DocumentType documentType);
    Optional<Document> findByIdAndUserEmail(Long id, String email);

}
