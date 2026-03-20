package com.hinetics.caresync.dto.user;

import com.hinetics.caresync.enums.CareGiverPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverRequestSendDto { // caregiver email
    private String receiveUserEmail; // patient email
    private CareGiverPermission requestedPermission;
}
