package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document,Long> {
}
