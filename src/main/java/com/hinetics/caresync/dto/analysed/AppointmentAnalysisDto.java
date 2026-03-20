package com.hinetics.caresync.dto.analysed;

import com.hinetics.caresync.enums.AppointmentType;
import com.hinetics.caresync.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentAnalysisDto {
    private Long id;
    private String name;
    private AppointmentType type;
    private DoctorAnalysisDto doctor;
    private LocalDateTime appointmentDateTime;
    private EntityStatus entityStatus;
}
