package com.hinetics.caresync.service;

import com.hinetics.caresync.entity.DeletedDocument;
import com.hinetics.caresync.repository.DeletedDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeleteDocumentService {
    private final DeletedDocumentRepository deletedDocumentRepository;

    // 🔹 Save deletion record
    public void saveDeletedDocument(Long documentId, Long userId) {
        DeletedDocument deleted = new DeletedDocument();
        deleted.setDocumentId(documentId);
        deleted.setUserId(userId);
        deleted.setDeletedAt(LocalDateTime.now());

        deletedDocumentRepository.save(deleted);
    }

    // 🔹 Get deleted document IDs for sync
    public List<Long> getDeletedDocumentIdsForUsers(List<Long> userIds, LocalDateTime lastSyncTime) {
        return deletedDocumentRepository
                .findByUserIdInAndDeletedAtAfter(userIds, lastSyncTime)
                .stream()
                .map(DeletedDocument::getDocumentId)
                .toList();
    }

    // 🔹 Cleanup old deleted records (e.g., 30 days)
    public void cleanupOldDeletedDocuments(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        deletedDocumentRepository.deleteByDeletedAtBefore(cutoff);
    }
}
