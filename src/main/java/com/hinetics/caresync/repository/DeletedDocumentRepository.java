package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.DeletedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeletedDocumentRepository extends JpaRepository<DeletedDocument, Long> {
    // 🔹 Get deleted document IDs after last sync
    List<DeletedDocument> findByUserIdInAndDeletedAtAfter(List<Long> userIds, LocalDateTime lastSyncTime);

    // 🔹 Optional: cleanup old records
    void deleteByDeletedAtBefore(LocalDateTime cutoffTime);
}
