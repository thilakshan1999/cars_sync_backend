package com.hinetics.caresync.dto.extracted;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorExtractedDto {
    private String name;
    private String specialization;
    private String phoneNumber;
    private String email;
    private String address;
}
