package com.hinetics.caresync.dto.extracted;

import com.hinetics.caresync.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExtractedDto {
    private String documentName;
    private DocumentType documentType;
    private String summary;
    private List<DoctorExtractedDto> doctors;
    private List<MedExtractedDto> medicines;
    private List<AppointmentExtractedDto> appointments;
    private  List<VitalExtractedDto> vitals;
}
