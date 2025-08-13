package com.hinetics.caresync.dto.extracted;

import com.hinetics.caresync.enums.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentExtractedDto {
    private String name;
    private AppointmentType type;
    private String doctorName;
    private LocalDateTime appointmentDateTime;
}
