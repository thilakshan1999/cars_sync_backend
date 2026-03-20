package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.service.ai.DocumentAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentAIController {
    private final  DocumentAIService documentAIService;

    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<String>> extractText(@RequestParam("file") MultipartFile file) {
        try {
            // Create temp file
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            String mimeType = file.getContentType();

            String extractedText = documentAIService.extractText(tempFile, mimeType);
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(new ApiResponse<>(true, "Text extracted successfully", extractedText));
        } catch (Exception e) {
            String userMessage;
            if (e.getMessage() != null && e.getMessage().contains("Unsupported MIME type")) {
                userMessage = "This document type is not supported. Please upload a PDF or image file.";
            } else {
                userMessage = e.getMessage();
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, userMessage, null));
        }
    }
}
