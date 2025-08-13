package com.hinetics.caresync.dto.analysed;

import com.hinetics.caresync.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAnalysisDto {
    private Long id;
    private String name;
    private String specialization;
    private String phoneNumber;
    private String email;
    private String address;
    private EntityStatus entityStatus;
}
