package com.hinetics.caresync.dto.analysed;

import com.hinetics.caresync.dto.VitalMeasurementDto;
import com.hinetics.caresync.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VitalAnalysisDto {
    private Long id;
    private String name;
    private Duration remindDuration;
    private LocalDateTime startDateTime;
    private String unit;
    private List<VitalMeasurementDto> measurements;
    private EntityStatus entityStatus;
}
