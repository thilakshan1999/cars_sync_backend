package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.extracted.DocumentExtractedDto;
import com.hinetics.caresync.service.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {
    private final GeminiService geminiService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<DocumentExtractedDto>> generateText(@RequestBody Map<String, String> body) {
        try {
            String prompt = body.get("prompt");
            DocumentExtractedDto result = geminiService.generateText(prompt);
            return ResponseEntity.ok(new ApiResponse<>(true, "Success", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }
}
