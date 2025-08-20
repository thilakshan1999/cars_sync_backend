package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.DocumentSummaryDto;
import com.hinetics.caresync.dto.analysed.*;
import com.hinetics.caresync.dto.extracted.DocumentExtractedDto;
import com.hinetics.caresync.entity.*;
import com.hinetics.caresync.enums.DocumentType;
import com.hinetics.caresync.repository.DocumentRepository;
import com.hinetics.caresync.service.ai.GeminiService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final GeminiService geminiService;
    private final DoctorService doctorService;
    private final VitalService vitalService;
    private final MedService medService;
    private final AppointmentService appointmentService;

    public void deleteDocumentById(Long id) {
        documentRepository.deleteById(id);
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
    }

    public List<DocumentSummaryDto> getAllDocumentsSummary(String type) {
        List<Document> documents ;

        if (type == null || type.trim().isEmpty() || type.equalsIgnoreCase("All")) {
            documents = documentRepository.findAll();
        }else {
            try {
                DocumentType docType = DocumentType.fromString(type);
                documents = documentRepository.findByDocumentType(docType);
            } catch (IllegalArgumentException ex) {
                // fallback: if invalid type provided, return empty list (or all if you prefer)
                documents = Collections.emptyList();
            }
        }
        return documents.stream()
                .map(doc -> {
                    DocumentSummaryDto summary = new DocumentSummaryDto();
                    summary.setId(doc.getId());
                    summary.setDocumentName(doc.getDocumentName());
                    summary.setDocumentType(doc.getDocumentType());
                    summary.setSummary(doc.getSummary());
                    summary.setUpdatedTime(doc.getUpdatedTime());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    public DocumentAnalysisDto analyzeDocument(String documentContent) throws Exception {
        DocumentExtractedDto result = geminiService.generateText(documentContent);

        if (result == null) {
            throw new IllegalStateException("Gemini returned no data");
        }

        DocumentAnalysisDto dto = new DocumentAnalysisDto();
        dto.setDocumentName(result.getDocumentName());
        dto.setDocumentType(result.getDocumentType());
        dto.setSummary(result.getSummary());

        List<DoctorAnalysisDto> doctors = doctorService.mapAll(result.getDoctors());
        dto.setDoctors(doctors);

        List<VitalAnalysisDto> vitals = vitalService.mapAll(result.getVitals());
        dto.setVitals(vitals);

        List<MedAnalysisDto> meds = medService.mapAll(result.getMedicines());
        dto.setMeds(meds);

        List<AppointmentAnalysisDto> appointments = appointmentService.mapAll(result.getAppointments(),doctors);
        dto.setAppointments(appointments);

        return dto;
    }

    public Document saveFromDto(DocumentAnalysisDto dto) {
        Document entity = new Document();
        entity.setDocumentName(dto.getDocumentName());
        entity.setDocumentType(dto.getDocumentType());
        entity.setSummary(dto.getSummary());

        // Map Doctors
        if (dto.getDoctors() != null) {
            List<Doctor> doctors = doctorService.processDoctors(dto.getDoctors());
            entity.setDoctors(doctors);
        }

        // Map Vitals
        if (dto.getVitals() != null) {
            List<Vital> vitals = vitalService.processVitals(dto.getVitals());
            entity.setVitals(vitals);
        }

        // Map Meds
        if (dto.getMeds() != null) {
            List<Med> meds = medService.processMeds(dto.getMeds());
            entity.setMedicines(meds);
        }

        //Mad Appointment
        if (dto.getAppointments() != null) {
            List<Appointment> appointments = appointmentService.processAppointment(dto.getAppointments(),entity.getDoctors());
            entity.setAppointments(appointments);
        }

        return documentRepository.save(entity);
    }
}

