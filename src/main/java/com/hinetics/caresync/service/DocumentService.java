package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.DocumentReferenceDto;
import com.hinetics.caresync.dto.DocumentSummaryDto;
import com.hinetics.caresync.dto.FileUploadResult;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
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
    private  final  FileStorageService fileStorageService;

    public void deleteDocumentById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        validateDocumentAccess(user, document,true);

        // 🆕 Delete the associated file
        deleteDocumentFile(document);

        documentRepository.delete(document);
    }

    public DocumentReferenceDto getDocumentWithSignedUrl(Long id, String email) {
        User user = userService.getUserByEmail(email);
        // Load document from DB
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        String fileUrl = fileStorageService.generateSignedUrl(document.getFileName());
        validateDocumentAccess(user, document,false);

        return new DocumentReferenceDto(
                document.getId(),
                document.getFileName(),
                document.getFileType(),
                fileUrl
        );
    }

    public Document getDocumentById(Long id, String email) {
        User user = userService.getUserByEmail(email);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        validateDocumentAccess(user, document,false);
        return document;
    }

    public List<DocumentSummaryDto> getAllDocumentsSummary(
            String type,
            Long patientId,
            String filterBy,
            String sortOrder,
            String email
    ) {
        //Get User
        User user = userService.getUserByEmail(email);
        User targetUser = resolveTargetUser(user, patientId, false);

        //Get Document
        List<Document> documents;

        if (type == null || type.trim().isEmpty() || type.equalsIgnoreCase("All")) {
            documents = documentRepository.findByUser(targetUser );
        } else {
            try {
                DocumentType docType = DocumentType.fromString(type);
                documents = documentRepository.findByUserAndDocumentType(targetUser, docType);
            } catch (IllegalArgumentException ex) {
                documents = Collections.emptyList();
            }
        }

        // Determine the field to filter/sort by
        Function<Document, LocalDateTime> sortKey = doc -> {
            switch (filterBy.toUpperCase()) {
                case "VISIT_TIME": return doc.getDateOfVisit();     // custom mapping
                case "TEST_TIME": return doc.getDateOfTest();     // custom mapping
                case "UPLOAD_TIME":
                default: return doc.getUpdatedTime();
            }
        };
        System.out.println("sortKey");
        System.out.println(sortKey);

        // Separate documents with null field from non-null
        List<Document> nonNullDocs = documents.stream()
                .filter(doc -> sortKey.apply(doc) != null)
                .collect(Collectors.toList());

        List<Document> nullDocs = documents.stream()
                .filter(doc -> sortKey.apply(doc) == null)
                .collect(Collectors.toList());

        // Sort non-null documents
        Comparator<Document> comparator = Comparator.comparing(sortKey);
        if ("DESCENDING".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        nonNullDocs.sort(comparator);

        // Combine sorted non-null + null documents at the end
        List<Document> sortedDocuments = new ArrayList<>();
        sortedDocuments.addAll(nonNullDocs);
        sortedDocuments.addAll(nullDocs);


        return sortedDocuments.stream()
                .map(doc -> {
                    DocumentSummaryDto summary = new DocumentSummaryDto();
                    summary.setId(doc.getId());
                    summary.setDocumentName(doc.getDocumentName());
                    summary.setDocumentType(doc.getDocumentType());
                    summary.setSummary(doc.getSummary());
                    summary.setUpdatedTime(doc.getUpdatedTime());
                    summary.setDateOfVisit(doc.getDateOfVisit());
                    summary.setDateOfTest(doc.getDateOfTest());
                    return summary;
                })
                .collect(Collectors.toList());
    }


    public DocumentAnalysisDto analyzeDocument(String documentContent,Long patientId,String email) throws Exception {
        User user = userService.getUserByEmail(email);

        User targetUser = resolveTargetUser(user, patientId, true);

        DocumentExtractedDto result = geminiService.generateText(documentContent);

        System.out.println("result");
        System.out.println(result);
        if (result == null) {
            throw new IllegalStateException("Gemini returned no data");
        }

        DocumentAnalysisDto dto = new DocumentAnalysisDto();
        dto.setDocumentName(result.getDocumentName());
        dto.setDocumentType(result.getDocumentTypeList().get(0));
        dto.setDocumentTypeList(result.getDocumentTypeList());
        dto.setSummary(result.getSummary());
        dto.setDateOfTest(result.getDateOfTest());
        dto.setDateOfVisit(result.getDateOfVisit());

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

    public void saveFromDto(DocumentAnalysisDto dto, MultipartFile file,Long patientId, String email) throws IOException {
        User user = userService.getUserByEmail(email);

        User targetUser = resolveTargetUser(user, patientId, true);

        Document entity = new Document();
        entity.setDocumentName(dto.getDocumentName());
        entity.setDocumentType(dto.getDocumentType());
        entity.setSummary(dto.getSummary());
        entity.setUser(targetUser);
        entity.setDateOfTest(dto.getDateOfTest());
        entity.setDateOfVisit(dto.getDateOfVisit());

        updateFileInfo(entity, file);

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

    public void deleteDocumentsByIds(List<Long> ids, String email) {
        User user = userService.getUserByEmail(email);

        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.isEmpty()) {
            throw new EntityNotFoundException("No documents found for provided IDs");
        }

        for (Document doc : documents) {
            validateDocumentAccess(user, doc, true);
            deleteDocumentFile(doc);
        }

        documentRepository.deleteAll(documents);
    }

    public List<DocumentReferenceDto> getDocumentsWithSignedUrls(List<Long> ids, String email) {
        User user = userService.getUserByEmail(email);

        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.isEmpty()) {
            throw new EntityNotFoundException("No documents found for provided IDs");
        }

        return documents.stream()
                .filter(doc -> {
                    try {
                        validateDocumentAccess(user, doc, false);
                        return true;
                    } catch (Exception e) {
                        return false; // skip inaccessible docs
                    }
                })
                .map(doc -> new DocumentReferenceDto(
                        doc.getId(),
                        doc.getFileName(),
                        doc.getFileType(),
                        fileStorageService.generateSignedUrl(doc.getFileName())
                ))
                .collect(Collectors.toList());
    }

    private void updateFileInfo(Document entity, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            // Upload file to Google Cloud Storage
            FileUploadResult result = fileStorageService.uploadFile(file);

            // Update entity fields
            entity.setFileUrl(result.getFileUrl());
            entity.setFileName(result.getFileName());
            entity.setFileType(result.getFileType());
        }
    }

    private void deleteDocumentFile(Document document) {
        if (document.getFileName() != null) {
            boolean deleted = fileStorageService.deleteFile(document.getFileName());
            if (!deleted) {
                System.out.println("Warning: File not found in GCS: " + document.getFileName());
            }
        }
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

