package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.*;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
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
    private  final  DeleteDocumentService deleteDocumentService;

    public void deleteDocumentById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        validateDocumentAccess(user, document,true);

        // 🆕 Delete the associated file
        deleteDocumentFile(document);

        deleteDocumentService.saveDeletedDocument(document.getId(),document.getUser().getId());
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

    public void saveFromDto(
            DocumentAnalysisDto dto,
            MultipartFile multipartFile,        // nullable
            FileUploadResult fileUploadResult,  // nullable
            Long patientId,
            String email
    ) throws IOException {

        User user = userService.getUserByEmail(email);
        User targetUser = resolveTargetUser(user, patientId, true);

        Document entity = new Document();
        entity.setDocumentName(dto.getDocumentName());
        entity.setDocumentType(dto.getDocumentType());
        entity.setSummary(dto.getSummary());
        entity.setUser(targetUser);
        entity.setDateOfTest(dto.getDateOfTest());
        entity.setDateOfVisit(dto.getDateOfVisit());

        // ✅ Handle file (only one will be present)
        handleFile(entity, multipartFile, fileUploadResult);

        // Doctors
        if (dto.getDoctors() != null) {
            List<Doctor> doctors = doctorService.processDoctors(dto.getDoctors(), targetUser);
            entity.setDoctors(doctors);
        }

        // Vitals
        if (dto.getVitals() != null) {
            List<Vital> vitals = vitalService.processVitals(dto.getVitals(), targetUser);
            entity.setVitals(vitals);
        }

        // Meds
        if (dto.getMeds() != null) {
            List<Med> meds = medService.processMeds(dto.getMeds(), targetUser);
            entity.setMedicines(meds);
        }

        // Appointments
        if (dto.getAppointments() != null) {
            List<Appointment> appointments =
                    appointmentService.processAppointment(dto.getAppointments(), entity.getDoctors(), user);
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
            deleteDocumentService.saveDeletedDocument(doc.getId(),doc.getUser().getId());
        }

        documentRepository.deleteAll(documents);
    }

    public List<DocumentReferenceDto> getDocumentsWithSignedUrls(List<Long> ids, String email) {
        User user = userService.getUserByEmail(email);

        List<Document> documents = documentRepository.findAllById(ids);
        if (documents.isEmpty()) {
            throw new EntityNotFoundException("No documents found for provided IDs");
        }

        List<DocumentReferenceDto> result = documents.stream()
                .filter(doc -> {
                    // Skip if required fields are missing
                    return doc.getFileName() != null && !doc.getFileName().isBlank()
                            && doc.getFileType() != null && !doc.getFileType().isBlank();
                })
                .filter(doc -> {
                    try {
                        // Skip if user cannot access the document
                        validateDocumentAccess(user, doc, false);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(doc -> {
                    try {
                        String signedUrl = fileStorageService.generateSignedUrl(doc.getFileName());
                        return new DocumentReferenceDto(
                                doc.getId(),
                                doc.getFileName(),
                                doc.getFileType(),
                                signedUrl
                        );
                    } catch (Exception e) {
                        // Skip if signed URL generation fails
                        return null;
                    }
                })
                .filter(Objects::nonNull) // remove any failed mappings
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            throw new EntityNotFoundException("No valid documents found for the provided IDs");
        }

        return result;
    }

    public DocumentSyncDto syncDocuments(String email, LocalDateTime lastSyncTime) {
        User user = userService.getUserByEmail(email);

        List<Long> ids = new ArrayList<>();

        if (user.getRole() == UserRole.PATIENT) {
            ids.add(user.getId());
        } else {
            ids = careGiverAssignmentService.getPatientIdsOfCaregiver(email);
        }

        List<Document> docs = documentRepository
                .findByUserIdInAndUpdatedTimeAfterOrderByUpdatedTimeAsc(ids, lastSyncTime);

        List<DocumentDto> documentDtoList =convertToDtoList(docs);

        List<Long> deleted = deleteDocumentService
                .getDeletedDocumentIdsForUsers(ids, lastSyncTime);

        LocalDateTime serverTime = LocalDateTime.now(ZoneOffset.UTC);

        return new DocumentSyncDto(documentDtoList, deleted, serverTime);
    }

    private void handleFile(
            Document entity,
            MultipartFile multipartFile,
            FileUploadResult fileUploadResult
    ) throws IOException {

        // 🔹 Case 1: New upload (old flow)
        if (multipartFile != null && !multipartFile.isEmpty()) {
            FileUploadResult result = fileStorageService.uploadFile(multipartFile);

            entity.setFileHash(result.getFileHash());
            entity.setFileName(result.getFileName());
            entity.setFileUrl(result.getFileUrl());
            entity.setFileType(result.getFileType());

        }

        // 🔹 Case 2: Already uploaded (new async flow)
        else if (fileUploadResult != null) {
            entity.setFileHash(fileUploadResult.getFileHash());
            entity.setFileName(fileUploadResult.getFileName());
            entity.setFileUrl(fileUploadResult.getFileUrl());
            entity.setFileType(fileUploadResult.getFileType());
        }

        // 🔹 Safety check
        else {
            throw new RuntimeException("No file provided");
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

    public List<DocumentDto> convertToDtoList(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        return documents.stream()
                .map(doc -> {
                    DocumentDto dto = DocumentDto.fromEntity(doc);

                    // Update fileUrl with signed URL if fileUrl exists
                    if (dto.getFileUrl() != null && !dto.getFileUrl().isEmpty()) {
                        dto.setFileUrl(fileStorageService.generateSignedUrl(dto.getFileName()));
                    }
                    return dto;
                })
                .toList();
    }

    public User resolveTargetUser(User user, Long patientId,boolean requireFullAccess) {
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

    public Boolean checkExistsByFileHash(String hash , Long patientId){
        return  documentRepository.existsByFileHashAndUser_Id(hash,patientId);
    }

    public Map<String, Object> checkDuplicate(
            MultipartFile file,
            Long patientId,
            String email
    ) throws IOException {

        User user = userService.getUserByEmail(email);
        User targetUser = resolveTargetUser(user, patientId, true);

        // ✅ Generate hash using your method
        String hash = fileStorageService.generateFileHash(file.getInputStream());

        boolean existsInDocs = checkExistsByFileHash(hash,targetUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("duplicate", existsInDocs);
        response.put("hash", hash);

        return response;
    }
}

