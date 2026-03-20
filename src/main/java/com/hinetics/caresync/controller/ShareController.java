package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<String>> processSharedDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            shareService.saveDocumentViaShare(file, patientId, email);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Document processed successfully", "")
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Processing failed: " + e.getMessage(), ""));
        }
    }
}
