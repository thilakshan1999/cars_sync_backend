package com.hinetics.caresync.dto.analysed;

import com.hinetics.caresync.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisDto {
    private String documentName;
    private DocumentType documentType;
    private List<DocumentType> documentTypeList;
    private String summary;
    private List<DoctorAnalysisDto> doctors;
    private List<VitalAnalysisDto> vitals;
    private List<MedAnalysisDto> meds;
    private List<AppointmentAnalysisDto> appointments;
    private LocalDateTime dateOfTest;
    private LocalDateTime dateOfVisit;
}
