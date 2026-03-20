package com.hinetics.caresync.service.user;

import com.hinetics.caresync.dto.user.CareGiverAssignmentDto;
import com.hinetics.caresync.dto.user.UserSummaryDto;
import com.hinetics.caresync.entity.CareGiverAssignment;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.CareGiverPermission;
import com.hinetics.caresync.repository.CareGiverAssignmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareGiverAssignmentService {
    private final CareGiverAssignmentRepository assignmentRepository;
    private final UserService userService;

    @Transactional
    public void assignCaregiverToPatient(User caregiver, User patient, CareGiverPermission permission) {
        // Prevent duplicate assignment
        if (assignmentRepository.existsByCaregiverAndPatient(caregiver, patient)) {
            throw new RuntimeException("Caregiver already assigned to this patient");
        }

        CareGiverAssignment assignment = new CareGiverAssignment();
        assignment.setCaregiver(caregiver);
        assignment.setPatient(patient);
        assignment.setPermission(permission);

        assignmentRepository.save(assignment);
    }

    public List<CareGiverAssignmentDto> getCaregiversOfPatient(String email) {
        User user = userService.getUserByEmail(email);

        return assignmentRepository.findByPatientId(user.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<CareGiverAssignmentDto> getPatientsOfCaregiver(String email) {
        User user = userService.getUserByEmail(email);

        return assignmentRepository.findByCaregiverId(user.getId()).stream()
                .map(this::mapToDto)
                .toList();
    }

    public void removeAssignment(Long assignmentId) {
        CareGiverAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        assignmentRepository.delete(assignment);
    }

    @Transactional
    public void updatePermission(Long assignmentId, CareGiverPermission permission, String requesterEmail) {
        CareGiverAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // only patient can update permission
        if (!assignment.getPatient().getEmail().equals(requesterEmail)) {
            throw new RuntimeException("Only the patient can update permission");
        }

        assignment.setPermission(permission);
    }

    private CareGiverAssignmentDto mapToDto(CareGiverAssignment assignment) {
        return new CareGiverAssignmentDto(
                assignment.getId(),
                new UserSummaryDto(
                        assignment.getCaregiver().getId(),
                        assignment.getCaregiver().getEmail(),
                        assignment.getCaregiver().getName(),
                        assignment.getCaregiver().getRole()
                ),
                new UserSummaryDto(
                        assignment.getPatient().getId(),
                        assignment.getPatient().getEmail(),
                        assignment.getPatient().getName(),
                        assignment.getPatient().getRole()
                ),
                assignment.getPermission()
        );
    }

    public User validateCaregiverAccess(String caregiverEmail, Long patientId,boolean requireFullAccess) {
        User caregiver = userService.getUserByEmail(caregiverEmail);

        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID must be provided for caregiver");
        }

        User patient = userService.getUserById(patientId);

        CareGiverAssignment assignment = assignmentRepository
                .findByCaregiverAndPatient(caregiver, patient)
                .orElseThrow(() -> new IllegalArgumentException("You are not assigned to this patient"));

        if (requireFullAccess && assignment.getPermission() != CareGiverPermission.FULL_ACCESS) {
            throw new IllegalArgumentException("You do not have full access to this patient's documents");
        }

        return patient;
    }

    public boolean isAssignedToPatient(User caregiver, User patient) {
        return assignmentRepository.existsByCaregiverAndPatient(caregiver, patient);
    }

    public CareGiverPermission getCaregiverPermission(User caregiver, User patient) {
        return assignmentRepository.findByCaregiverAndPatient(caregiver, patient)
                .map(CareGiverAssignment::getPermission)
                .orElseThrow(() -> new IllegalArgumentException("You are not assigned to this patient"));
    }
}
