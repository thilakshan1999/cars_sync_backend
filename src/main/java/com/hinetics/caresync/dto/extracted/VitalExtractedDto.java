package com.hinetics.caresync.dto.extracted;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VitalExtractedDto {
    private String name;
    private String unit;
    private String value;
    private LocalDateTime dateTime;
}
