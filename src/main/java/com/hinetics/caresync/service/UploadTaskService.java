package com.hinetics.caresync.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.hinetics.caresync.dto.FileUploadResult;
import com.hinetics.caresync.dto.analysed.DocumentAnalysisDto;
import com.hinetics.caresync.entity.UploadTask;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.repository.UploadTaskRepository;
import com.hinetics.caresync.service.ai.DocumentAIService;
import com.hinetics.caresync.service.ai.GeminiService;
import com.hinetics.caresync.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadTaskService {
    private final UploadTaskRepository uploadTaskRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final DocumentService documentService;
    private final DocumentAIService documentAIService;
    private final GeminiService geminiService;

    public UploadTask createTask(
            String fileName,
            String fileUrl,
            String mimeType,
            String hash,
            Long patientId,
            String email,
            boolean isDuplicate
    ) {

        UploadTask task = UploadTask.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .mimeType(mimeType)
                .patientId(patientId)
                .fileHash(hash)
                .createdBy(email)
                .status(isDuplicate ? "PAUSED" : "PROCESSING")
                .errorMessage(isDuplicate ? "This document already exists" : null)
                .build();

        return uploadTaskRepository.save(task);
    }

    public void markCompleted(Long taskId) {
        uploadTaskRepository.deleteById(taskId);
    }

    public void markFailed(Long taskId, String error) {
        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow();

        task.setStatus("FAILED");
        task.setErrorMessage(error);

        uploadTaskRepository.save(task);
    }

    public UploadTask getTaskByIdForUser(Long taskId,Long patientId, String email) {
        User user = userService.getUserByEmail(email);
        User targetUser = documentService.resolveTargetUser(user, patientId, true);

        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getPatientId().equals(targetUser.getId())) {
            throw new RuntimeException("Unauthorized access");
        }
        String fileUrl = fileStorageService.generateSignedUrl(task.getFileName());
        task.setFileUrl(fileUrl);
        return task;
    }

    public List<UploadTask> getTasksByUser(Long patientId,String email) {
        User user = userService.getUserByEmail(email);
        User targetUser = documentService.resolveTargetUser(user, patientId, false);

        return uploadTaskRepository.findByPatientIdOrderByIdDesc(targetUser.getId());
    }

    public void deleteTaskWithFile(Long taskId,Long patientId, String email) {
        User user = userService.getUserByEmail(email);
        User targetUser = documentService.resolveTargetUser(user, patientId, true);

        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getPatientId().equals(targetUser.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        if (task.getFileName() != null) {
            fileStorageService.deleteFile(task.getFileName());
        }

        uploadTaskRepository.delete(task);
    }

    public void retryTask(Long taskId, Long patientId, String email) {

        UploadTask task = getAndValidateTask(taskId, patientId, email);

        try {
            markProcessing(task);

            Path tempFile = downloadFromGCS(task.getFileName());
            String extractedText = extractText(tempFile, task.getMimeType());

            processAndSaveDocument(extractedText, task, patientId, email);

            cleanupTempFile(tempFile);

            markCompleted(task.getId());

        } catch (Exception e) {
            handleRetryFailure(task, e);
            throw new RuntimeException("Retry failed: " + e.getMessage());
        }
    }

    private UploadTask getAndValidateTask(Long taskId, Long patientId, String email) {

        User user = userService.getUserByEmail(email);
        User targetUser = documentService.resolveTargetUser(user, patientId, true);

        UploadTask task = uploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getPatientId().equals(targetUser.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        if (!"FAILED".equals(task.getStatus())) {
            throw new RuntimeException("Only failed tasks can be retried");
        }

        return task;
    }

    private void markProcessing(UploadTask task) {
        task.setStatus("PROCESSING");
        task.setErrorMessage(null);
        uploadTaskRepository.save(task);
    }

    private void processAndSaveDocument(
            String extractedText,
            UploadTask task,
            Long patientId,
            String email
    ) throws Exception {

        DocumentAnalysisDto dto =
                documentService.analyzeDocument(extractedText, patientId, email);

        FileUploadResult uploadResult = new FileUploadResult();
        uploadResult.setFileHash(task.getFileHash());
        uploadResult.setFileName(task.getFileName());
        uploadResult.setFileType(task.getMimeType());
        uploadResult.setFileUrl(task.getFileUrl());

        documentService.saveFromDto(dto, null, uploadResult, patientId, email);
    }

    private void cleanupTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {}
    }

    private void handleRetryFailure(UploadTask task, Exception e) {
        task.setStatus("FAILED");
        task.setErrorMessage(e.getMessage());
        uploadTaskRepository.save(task);
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
