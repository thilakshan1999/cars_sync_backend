package com.hinetics.caresync.service.user;

import com.hinetics.caresync.dto.AuthResponseDto;
import com.hinetics.caresync.dto.user.LoginRequestDto;
import com.hinetics.caresync.dto.user.UserRegistrationDto;
import com.hinetics.caresync.dto.user.UserSummaryDto;
import com.hinetics.caresync.entity.CareGiverAssignment;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.CareGiverPermission;
import com.hinetics.caresync.enums.UserRole;
import com.hinetics.caresync.repository.UserRepository;
import com.hinetics.caresync.security.JwtTokenUtil;
import com.hinetics.caresync.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final MailService mailService;

    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public AuthResponseDto register(UserRegistrationDto dto) {
        String normalizedEmail = dto.getEmail().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setRole(dto.getRole());
        user.setName(dto.getName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setDateOfBirth(dto.getDateOfBirth());

        // Generate refresh token
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);

        userRepository.save(user);

        String accessToken = jwtTokenUtil.generateToken(user.getEmail(), user.getName(), user.getRole().name());

        return new AuthResponseDto(accessToken, refreshToken);
    }

    public AuthResponseDto  loginAndGenerateToken(LoginRequestDto dto) {
        User user = userRepository.findByEmailIgnoreCase(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String  refreshToken =user.getRefreshToken();
        String accessToken =  jwtTokenUtil.generateToken(user.getEmail(), user.getName(), user.getRole().name());

        return new AuthResponseDto(accessToken, refreshToken);
    }

    public AuthResponseDto refreshAccessToken(String requestRefreshToken) {
        User user = userRepository.findByRefreshToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        String accessToken = jwtTokenUtil.generateToken(user.getEmail(), user.getName(), user.getRole().name());

        return new AuthResponseDto(accessToken, requestRefreshToken);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void sendResetOtp(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));

        // Hash OTP before saving
        String hashedOtp = passwordEncoder.encode(otp);
        user.setOtpHash(hashedOtp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setOtpAttempts(0);
        userRepository.save(user);

        // Send OTP via email
        String subject = "Password Reset OTP";
        String text = """
                Your OTP code to reset your password is: %s
                It expires in 10 minutes.
                """.formatted(otp);

        mailService.sendMail(user.getEmail(), subject, text);
    }

    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (user.getOtpAttempts() >= 3) {
            throw new RuntimeException("Too many invalid attempts");
        }

        if (!passwordEncoder.matches(otp, user.getOtpHash())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new RuntimeException("Invalid OTP");
        }

    }

    public void ResetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (user.getOtpAttempts() >= 3) {
            throw new RuntimeException("Too many invalid attempts");
        }

        if (!passwordEncoder.matches(otp, user.getOtpHash())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new RuntimeException("Invalid OTP");
        }

        // OTP valid → reset password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtpHash(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        userRepository.save(user);
    }

    public List<UserSummaryDto> getPatientsWithFullAccess(String email) {
        User caregiver = getUserByEmail(email);

        if (caregiver.getRole() != UserRole.CAREGIVER) {
            throw new RuntimeException("User is not a caregiver");
        }

        List<CareGiverAssignment> fullAccessAssignments = caregiver.getAssignedPatients().stream()
                .filter(assignment -> assignment.getPermission() == CareGiverPermission.FULL_ACCESS)
                .toList();

        return fullAccessAssignments.stream()
                .map(CareGiverAssignment::getPatient) // get the patient User
                .map(patient -> new UserSummaryDto(patient.getId(), patient.getEmail(),patient.getName(),patient.getRole()))
                .collect(Collectors.toList());
    }
}
