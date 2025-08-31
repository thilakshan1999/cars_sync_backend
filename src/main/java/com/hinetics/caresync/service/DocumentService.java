package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.DocumentSummaryDto;
import com.hinetics.caresync.dto.analysed.*;
import com.hinetics.caresync.dto.extracted.DocumentExtractedDto;
import com.hinetics.caresync.entity.*;
import com.hinetics.caresync.enums.CareGiverPermission;
import com.hinetics.caresync.enums.DocumentType;
import com.hinetics.caresync.enums.UserRole;
import com.hinetics.caresync.repository.DocumentRepository;
import com.hinetics.caresync.service.ai.GeminiService;
import com.hinetics.caresync.service.user.CareGiverAssignmentService;
import com.hinetics.caresync.service.user.UserService;
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
    private  final UserService userService;
    private final GeminiService geminiService;
    private final DoctorService doctorService;
    private final VitalService vitalService;
    private final MedService medService;
    private final AppointmentService appointmentService;
    private  final CareGiverAssignmentService careGiverAssignmentService;

    public void deleteDocumentById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        validateDocumentAccess(user, document,true);

        documentRepository.delete(document);
    }

    public Document getDocumentById(Long id, String email) {
        User user = userService.getUserByEmail(email);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        validateDocumentAccess(user, document,false);
        return document;
    }

    public List<DocumentSummaryDto> getAllDocumentsSummary(String type,Long patientId,String email) {
        List<Document> documents ;

        User user = userService.getUserByEmail(email);

        User targetUser = resolveTargetUser(user, patientId,false);

        if (type == null || type.trim().isEmpty() || type.equalsIgnoreCase("All")) {
            documents = documentRepository.findByUser(targetUser);
        }else {
            try {
                DocumentType docType = DocumentType.fromString(type);
                documents =  documentRepository.findByUserAndDocumentType(targetUser, docType);
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

    public DocumentAnalysisDto analyzeDocument(String documentContent,Long patientId,String email) throws Exception {
        User user = userService.getUserByEmail(email);

        User targetUser = resolveTargetUser(user, patientId, true);

        DocumentExtractedDto result = geminiService.generateText(documentContent);

        if (result == null) {
            throw new IllegalStateException("Gemini returned no data");
        }

        DocumentAnalysisDto dto = new DocumentAnalysisDto();
        dto.setDocumentName(result.getDocumentName());
        dto.setDocumentType(result.getDocumentType());
        dto.setSummary(result.getSummary());

        List<DoctorAnalysisDto> doctors = doctorService.mapAll(result.getDoctors(),targetUser);
        dto.setDoctors(doctors);

        List<VitalAnalysisDto> vitals = vitalService.mapAll(result.getVitals(),targetUser);
        dto.setVitals(vitals);

        List<MedAnalysisDto> meds = medService.mapAll(result.getMedicines(),targetUser);
        dto.setMeds(meds);

        List<AppointmentAnalysisDto> appointments = appointmentService.mapAll(result.getAppointments(),doctors,targetUser);
        dto.setAppointments(appointments);

        return dto;
    }

    public void saveFromDto(DocumentAnalysisDto dto,Long patientId,String email) {
        User user = userService.getUserByEmail(email);

        User targetUser = resolveTargetUser(user, patientId, true);

        Document entity = new Document();
        entity.setDocumentName(dto.getDocumentName());
        entity.setDocumentType(dto.getDocumentType());
        entity.setSummary(dto.getSummary());
        entity.setUser(targetUser);

        // Map Doctors
        if (dto.getDoctors() != null) {
            List<Doctor> doctors = doctorService.processDoctors(dto.getDoctors(),targetUser);
            entity.setDoctors(doctors);
        }

        // Map Vitals
        if (dto.getVitals() != null) {
            List<Vital> vitals = vitalService.processVitals(dto.getVitals(),targetUser);
            entity.setVitals(vitals);
        }

        // Map Meds
        if (dto.getMeds() != null) {
            List<Med> meds = medService.processMeds(dto.getMeds(),targetUser);
            entity.setMedicines(meds);
        }

        //Mad Appointment
        if (dto.getAppointments() != null) {
            List<Appointment> appointments = appointmentService.processAppointment(dto.getAppointments(),entity.getDoctors(),user);
            entity.setAppointments(appointments);
        }

        documentRepository.save(entity);
    }

    private void validateDocumentAccess(User user, Document document,boolean requireFullAccess) {
        if (user.getRole() == UserRole.PATIENT) {
            if (!document.getUser().getId().equals(user.getId())) {
                throw new EntityNotFoundException("You are not allowed to access this document");
            }
        } else if (user.getRole() == UserRole.CAREGIVER) {
            CareGiverPermission permission = careGiverAssignmentService.getCaregiverPermission(user, document.getUser());

            if (requireFullAccess && permission != CareGiverPermission.FULL_ACCESS) {
                throw new IllegalArgumentException("You only have view access, not full access");
            }
        } else {
            throw new IllegalArgumentException("Invalid role: " + user.getRole());
        }
    }

    private User resolveTargetUser(User user, Long patientId,boolean requireFullAccess) {
        if (user.getRole() == UserRole.CAREGIVER) {
            // delegate validation to CareGiverAssignmentService
            return careGiverAssignmentService.validateCaregiverAccess(
                    user.getEmail(),
                    patientId,
                    requireFullAccess
            );
        }
        return user; // patient accessing own documents
    }
}

