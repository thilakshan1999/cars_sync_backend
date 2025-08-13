package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.DocumentSummaryDto;
import com.hinetics.caresync.dto.analysed.DocumentAnalysisDto;
import com.hinetics.caresync.entity.Document;
import com.hinetics.caresync.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<DocumentAnalysisDto>> analyzeDocument(@RequestBody Map<String, String> body) {
        try {
            String prompt = body.get("prompt");
            DocumentAnalysisDto dto = documentService.analyzeDocument(prompt);
            return ResponseEntity.ok(new ApiResponse<>(true, "Success", dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocumentById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Document deleted successfully", null));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to delete document", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Document>> getDocumentById(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "Document fetched successfully", document));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to fetch document", null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> getAllDocumentsSummary() {
        try {
            List<DocumentSummaryDto> summaries = documentService.getAllDocumentsSummary();
            return ResponseEntity.ok(new ApiResponse<>(true, "Documents fetched successfully", summaries));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to fetch documents", null));
        }
    }


    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveDocument(@RequestBody DocumentAnalysisDto dto) {
        try {
            documentService.saveFromDto(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Document saved successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error saving document: " + e.getMessage(), null));
        }
    }


}
