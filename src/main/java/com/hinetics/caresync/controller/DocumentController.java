package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.DocumentReferenceDto;
import com.hinetics.caresync.dto.DocumentSummaryDto;
import com.hinetics.caresync.dto.DocumentSyncDto;
import com.hinetics.caresync.dto.analysed.DocumentAnalysisDto;
import com.hinetics.caresync.entity.Document;
import com.hinetics.caresync.service.DocumentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<DocumentAnalysisDto>> analyzeDocument(
            @RequestBody Map<String, String> body,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email) {
        try {
            String prompt = body.get("prompt");
            DocumentAnalysisDto dto = documentService.analyzeDocument(prompt,patientId,email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Success", dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id,@AuthenticationPrincipal String email) {
        try {
            documentService.deleteDocumentById(id,email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Document deleted successfully", null));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to delete document", null));
        }
    }

    @GetMapping("/{id}/ref")
    public ResponseEntity<ApiResponse<DocumentReferenceDto>> viewDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal String email ) {
        try {
            DocumentReferenceDto dto = documentService.getDocumentWithSignedUrl(id, email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Document reference fetched successfully", dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to fetch document reference", null));
        }


    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Document>> getDocumentById(@PathVariable Long id,@AuthenticationPrincipal String email) {
        try {
            Document document = documentService.getDocumentById(id,email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Document fetched successfully", document));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to fetch document", null));
        }
    }

    @GetMapping("/sync")
    public ResponseEntity<ApiResponse<DocumentSyncDto>> syncDocuments(
            @AuthenticationPrincipal String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime lastSyncTime
    ) {
        try {
            DocumentSyncDto response = documentService.syncDocuments(email, lastSyncTime);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Documents sync fetched successfully", response)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to fetch documents sync", null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentSummaryDto>>> getAllDocumentsSummary(
            @RequestParam(value = "type", required = false) String type ,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "filterBy", required = false, defaultValue = "UPLOAD_TIME") String filterBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "DESCENDING") String sortOrder,
            @AuthenticationPrincipal String email
    ) {
        try {
            List<DocumentSummaryDto> summaries = documentService.getAllDocumentsSummary(type,patientId,filterBy,sortOrder,email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Documents fetched successfully", summaries));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to fetch documents", null));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Void>> saveDocument(
            @RequestPart("dto") DocumentAnalysisDto dto,
            @RequestPart(value = "file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            documentService.saveFromDto(dto,file,null,patientId,email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Document saved successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error saving document: " + e.getMessage(), null));
        }
    }

    // Delete multiple documents
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<ApiResponse<Void>> deleteDocuments(
            @RequestBody List<Long> ids,
            @AuthenticationPrincipal String email) {
        try {
            documentService.deleteDocumentsByIds(ids, email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Documents deleted successfully", null));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to delete documents", null));
        }
    }

    // Get file URLs for a list of IDs
    @PostMapping("/file-urls")
    public ResponseEntity<ApiResponse<List<DocumentReferenceDto>>> getDocumentUrls(
            @RequestBody List<Long> ids,
            @AuthenticationPrincipal String email) {
        try {
            List<DocumentReferenceDto> files = documentService.getDocumentsWithSignedUrls(ids, email);
            return ResponseEntity.ok(new ApiResponse<>(true, "File URLs fetched successfully", files));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkDuplicate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            Map<String, Object> result =
                    documentService.checkDuplicate(file, patientId, email);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Checked successfully", result)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
