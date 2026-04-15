package com.hinetics.caresync.dto;

import com.hinetics.caresync.entity.*;
import com.hinetics.caresync.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private Long id;
    private String documentName;
    private DocumentType documentType;

    private String summary;

    private LocalDateTime dateOfTest;
    private LocalDateTime dateOfVisit;
    private LocalDateTime updatedTime;

    private String fileUrl;   // local file path or server URL
    private String fileName;
    private String fileType;

    private Long userId;

    private List<Doctor> doctors;
    private List<Vital> vitals;
    private List<Med> medicines;
    private List<Appointment> appointments;

    public static DocumentDto fromEntity(Document document) {
        return new DocumentDto(
                document.getId(),
                document.getDocumentName(),
                document.getDocumentType(),
                document.getSummary(),
                document.getDateOfTest(),
                document.getDateOfVisit(),
                document.getUpdatedTime(),
                document.getFileUrl(),
                document.getFileName(),
                document.getFileType(),
                document.getUser().getId(),
                document.getDoctors(),
                document.getVitals(),
                document.getMedicines(),
                document.getAppointments()
        );
    }
}
