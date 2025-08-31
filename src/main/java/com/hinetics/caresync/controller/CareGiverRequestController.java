package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.user.CareGiverRequestDto;
import com.hinetics.caresync.dto.user.CareGiverRequestSendDto;
import com.hinetics.caresync.service.user.CareGiverRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caregiver-requests")
@RequiredArgsConstructor
public class CareGiverRequestController {
    private final CareGiverRequestService requestService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('CAREGIVER')")
    public ResponseEntity<ApiResponse<Void>> sendRequest(
            @AuthenticationPrincipal String caregiverEmail,
            @RequestBody CareGiverRequestSendDto dto
    ) {
        try {
            requestService.sendRequest(caregiverEmail, dto);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request sent successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/sent")
    @PreAuthorize("hasRole('CAREGIVER')")
    public ResponseEntity<ApiResponse<List<CareGiverRequestDto>>> getSentRequests(
            @AuthenticationPrincipal String caregiverEmail

    ) {
        try {
            List<CareGiverRequestDto> requests = requestService.getSentRequests(
                    caregiverEmail
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Sent requests fetched", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/received")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<CareGiverRequestDto>>> getReceivedRequests(
            @AuthenticationPrincipal String patientEmail
    ) {
        try {
            List<CareGiverRequestDto> requests = requestService.getReceivedRequests(
                    patientEmail
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Received requests fetched", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{requestId}/accept")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(
            @AuthenticationPrincipal String patientEmail,
            @PathVariable Long requestId
    ) {
        try {
            requestService.acceptRequest(patientEmail, requestId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request accepted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @AuthenticationPrincipal String patientEmail,
            @PathVariable Long requestId
    ) {
        try {
            requestService.rejectRequest(patientEmail, requestId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request rejected", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
