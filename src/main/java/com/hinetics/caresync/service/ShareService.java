package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.analysed.DocumentAnalysisDto;
import com.hinetics.caresync.service.ai.DocumentAIService;
import com.hinetics.caresync.service.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class ShareService {
    private final GeminiService geminiService;
    private final DocumentAIService documentAIService;
    private final DocumentService documentService;

    public void saveDocumentViaShare(MultipartFile file, Long id, String email) throws Exception {
        String extractedText=extractText(file);
        DocumentAnalysisDto dto = documentService.analyzeDocument(extractedText,id,email);
        documentService.saveFromDto(dto,file,id,email);
    }

    private String extractText(MultipartFile file) {
        String mimeType = file.getContentType();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "temp";

        if (mimeType == null) {
            throw new RuntimeException("Unable to determine file type");
        }

        System.out.println();

        Path tempFile = null;

        try {
            // ✅ Create temp file
            tempFile = Files.createTempFile("upload-", fileName);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Temp file path: " + tempFile);
            System.out.println("Temp file size: " + Files.size(tempFile));

            // 📄 PDF
            if ("application/pdf".equals(mimeType)) {
                return documentAIService.extractText(tempFile, mimeType);
            }

            // 🖼 Image
            else if (mimeType.startsWith("image/")) {
                return geminiService.extractTextFromImage(tempFile);
            }

            else {
                throw new RuntimeException("Unsupported file type: " + mimeType);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text: " + e.getMessage(), e);

        } finally {
            try {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception ignored) {}
        }
    }
}
