package com.hinetics.caresync.service.user;

import com.hinetics.caresync.dto.AuthResponseDto;
import com.hinetics.caresync.dto.user.LoginRequestDto;
import com.hinetics.caresync.dto.user.UserRegistrationDto;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.repository.UserRepository;
import com.hinetics.caresync.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public AuthResponseDto register(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = new User();
        user.setRole(dto.getRole());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
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
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String  refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}
