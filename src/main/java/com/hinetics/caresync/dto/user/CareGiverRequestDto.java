package com.hinetics.caresync.dto.user;

import com.hinetics.caresync.enums.CareGiverPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareGiverRequestDto {
    private Long id;
    private UserSummaryDto fromUser; // sender
    private UserSummaryDto toUser;   // receiver
    private LocalDateTime requestedAt;
    private CareGiverPermission permission;
}
