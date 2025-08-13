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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to extract text: " + e.getMessage(), null));
        }
    }
}
