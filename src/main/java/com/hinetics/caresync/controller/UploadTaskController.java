package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.entity.UploadTask;
import com.hinetics.caresync.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/upload-tasks")
@RequiredArgsConstructor
public class UploadTaskController {
    private final UploadTaskService uploadTaskService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UploadTask>> getTaskById(
            @PathVariable Long id,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            UploadTask task = uploadTaskService.getTaskByIdForUser(id,patientId,email);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Task fetched successfully", task)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ✅ Get all tasks for logged-in user
    @GetMapping
    public ResponseEntity<ApiResponse<List<UploadTask>>> getUserTasks(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            List<UploadTask> tasks = uploadTaskService.getTasksByUser(patientId,email);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Tasks fetched successfully", tasks)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ✅ Delete task + file
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTask(
            @PathVariable Long id,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            uploadTaskService.deleteTaskWithFile(id,patientId, email);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Task deleted successfully", null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<String>> retryTask(
            @PathVariable Long id,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @AuthenticationPrincipal String email
    ) {
        try {
            uploadTaskService.retryTask(id,patientId, email);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Document update successfully",null )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(),null ));
        }
    }
}
