package com.hinetics.caresync.controller;

import com.hinetics.caresync.dto.ApiResponse;
import com.hinetics.caresync.dto.AuthResponseDto;
import com.hinetics.caresync.dto.user.LoginRequestDto;
import com.hinetics.caresync.dto.user.ResetPasswordRequest;
import com.hinetics.caresync.dto.user.UserRegistrationDto;
import com.hinetics.caresync.dto.user.UserSummaryDto;
import com.hinetics.caresync.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        try {
            boolean exists = userService.checkEmail(email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Email check successful", exists));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(@RequestBody UserRegistrationDto dto) {
        try {
            AuthResponseDto token= userService.register(dto);
            return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Registration failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@RequestBody LoginRequestDto dto) {
        try {
            AuthResponseDto token = userService.loginAndGenerateToken(dto); // void method now
            return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, "Invalid credentials", null));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Refresh token is required", null));
            }

            AuthResponseDto tokens = userService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(new ApiResponse<>(true, "Token refreshed", tokens));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid or expired refresh token", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "An error occurred: " + e.getMessage(), null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        try {
            System.out.println("test");
            userService.sendResetOtp(email);
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOpt(
            @RequestParam String email,
            @RequestParam String otp
            ) {
        try {
            userService.verifyOtp(
                    email,
                    otp
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP verified", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/patients/full-access")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getPatientsWithFullAccess( @AuthenticationPrincipal String email) {
        try {
            List<UserSummaryDto> patientList = userService.getPatientsWithFullAccess(email);
            return ResponseEntity.ok(new ApiResponse<>(true, "Patient list fetched successfully", patientList));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        try {
            userService.ResetPassword(
                    request.getEmail(),
                    request.getOtp(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successful", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
