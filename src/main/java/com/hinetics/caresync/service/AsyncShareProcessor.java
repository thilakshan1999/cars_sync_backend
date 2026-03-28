package com.hinetics.caresync.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.hinetics.caresync.dto.FileUploadResult;
import com.hinetics.caresync.dto.analysed.DocumentAnalysisDto;
import com.hinetics.caresync.service.ai.DocumentAIService;
import com.hinetics.caresync.service.ai.GeminiService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class AsyncShareProcessor {
    private final UploadTaskService uploadTaskService;
    private final DocumentService documentService;
    private final DocumentAIService documentAIService;
    private final GeminiService geminiService;

    @Async
    @Transactional
    public void processDocumentAsync(
            Long taskId,
            FileUploadResult uploadResult,
            Long patientId,
            String email
    ) {
        try {
            Path tempFile = downloadFromGCS(uploadResult.getFileName());

            String extractedText = extractText(tempFile, uploadResult.getFileType());


            DocumentAnalysisDto dto =
                    documentService.analyzeDocument(extractedText, patientId, email);

            documentService.saveFromDto(dto, null,uploadResult, patientId, email);

            uploadTaskService.markCompleted(taskId);

            Files.deleteIfExists(tempFile);

        } catch (Exception e) {
            uploadTaskService.markFailed(taskId, e.getMessage());
        }
    }

    private Path downloadFromGCS(String fileName) throws IOException {
        Path tempFile = Files.createTempFile("gcs-", fileName);

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Blob blob = storage.get("care-sync-bucket", fileName);

        blob.downloadTo(tempFile);

        return tempFile;
    }

    private String extractText(Path filePath, String mimeType) throws Exception {

        if ("application/pdf".equals(mimeType)) {
            return documentAIService.extractText(filePath, mimeType);
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            return geminiService.extractTextFromImage(filePath);
        } else {
            throw new RuntimeException("Unsupported file type: " + mimeType);
        }
    }
}
