package com.hinetics.caresync.dto.user;

import com.hinetics.caresync.enums.CareGiverPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareGiverAssignmentDto {
    private Long id;
    private UserSummaryDto caregiver; // sender
    private UserSummaryDto patient;   // receiver
    private CareGiverPermission permission;
}
