package com.hinetics.caresync.dto.user;

import com.hinetics.caresync.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDto {
    private UserRole role;
    private String name;
    private String email;
    private String password;
    private LocalDate dateOfBirth;
}
