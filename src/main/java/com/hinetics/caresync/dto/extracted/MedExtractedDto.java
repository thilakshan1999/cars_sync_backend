package com.hinetics.caresync.dto.extracted;

import com.hinetics.caresync.enums.IntakeInstruction;
import com.hinetics.caresync.enums.MedForm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedExtractedDto {
    private String medName;
    private MedForm medForm;
    private String healthCondition;
    private Duration intakeInterval;
    private LocalDateTime startDate;
    private LocalDate endDate;
    private String dosage;
    private IntakeInstruction instruction;
}
