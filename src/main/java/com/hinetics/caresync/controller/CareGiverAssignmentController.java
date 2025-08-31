package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.user.CareGiverAssignmentDto;
import com.hinetics.caresync.entity.CareGiverAssignment;
import com.hinetics.caresync.enums.CareGiverPermission;
import com.hinetics.caresync.service.user.CareGiverAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class CareGiverAssignmentController {
    private final CareGiverAssignmentService assignmentService;

    @GetMapping("/patient/caregivers")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<CareGiverAssignmentDto>>> getCaregiversOfPatient(
            @AuthenticationPrincipal String email) {
        try {

            List<CareGiverAssignmentDto> caregivers = assignmentService.getCaregiversOfPatient(email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Caregivers fetched successfully", caregivers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }


    @GetMapping("/caregiver/patients")
    @PreAuthorize("hasRole('CAREGIVER')")
    public ResponseEntity<ApiResponse<List<CareGiverAssignmentDto>>> getPatientsOfCaregiver(
            @AuthenticationPrincipal String email) {
        try {
            List<CareGiverAssignmentDto> patients = assignmentService.getPatientsOfCaregiver(email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Patients fetched successfully", patients));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(@PathVariable Long assignmentId) {
        try {
            assignmentService.removeAssignment(assignmentId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Assignment removed successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PutMapping("/{assignmentId}/permission")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> updatePermission(
            @PathVariable Long assignmentId,
            @RequestParam CareGiverPermission permission,
            @AuthenticationPrincipal String email // the one performing update
    ) {
        try {
            assignmentService.updatePermission(assignmentId, permission, email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Permission update successfully", null));
        }catch (Exception e){
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }

    }
}
