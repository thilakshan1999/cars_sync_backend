package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.user.CareGiverRequestDto;
import com.hinetics.caresync.dto.user.CaregiverRequestSendDto;
import com.hinetics.caresync.service.user.CareGiverRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/caregiver-requests")
@RequiredArgsConstructor
public class CareGiverRequestController {
    private final CareGiverRequestService requestService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendRequest(
            @AuthenticationPrincipal String senderEmail,
            @RequestBody CaregiverRequestSendDto dto
    ) {
        try {
            requestService.sendRequest(senderEmail, dto);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request sent successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<List<CareGiverRequestDto>>> getSentRequests(
            @AuthenticationPrincipal String senderEmail

    ) {
        try {
            List<CareGiverRequestDto> requests = requestService.getSentRequests(
                    senderEmail
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Sent requests fetched", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<CareGiverRequestDto>>> getReceivedRequests(
            @AuthenticationPrincipal String receiverEmail
    ) {
        try {
            List<CareGiverRequestDto> requests = requestService.getReceivedRequests(
                    receiverEmail
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Received requests fetched", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(
            @AuthenticationPrincipal String receiverEmail,
            @PathVariable Long requestId
    ) {
        try {
            requestService.acceptRequest(receiverEmail, requestId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request accepted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @AuthenticationPrincipal String receiverEmail,
            @PathVariable Long requestId
    ) {
        try {
            requestService.rejectRequest(receiverEmail, requestId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request rejected", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/generate-qr")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<String>> generateQrToken(
            @AuthenticationPrincipal String patientEmail,
            @RequestParam String permission // VIEW_ONLY / FULL_ACCESS
    ) {
        try {
            String qrToken = requestService.generateQrToken(patientEmail, permission);
            return ResponseEntity.ok(new ApiResponse<>(true, "QR token generated", qrToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/link-via-qr")
    @PreAuthorize("hasRole('CAREGIVER')")
    public ResponseEntity<ApiResponse<Void>> linkViaQr(
            @AuthenticationPrincipal String caregiverEmail,
            @RequestBody Map<String, String> request
    ) {
        try {
            String qrToken = request.get("qrToken");
            requestService.linkAccountViaQr(caregiverEmail, qrToken);
            return ResponseEntity.ok(new ApiResponse<>(true, "Patient linked successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
