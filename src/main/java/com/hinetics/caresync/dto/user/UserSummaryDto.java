package com.hinetics.caresync.dto.user;

import com.hinetics.caresync.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String email;
    private String systemEmail;
    private String name;
    private UserRole role;
}
