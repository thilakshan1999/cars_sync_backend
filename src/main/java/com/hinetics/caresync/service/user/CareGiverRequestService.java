package com.hinetics.caresync.service.user;

import com.hinetics.caresync.dto.user.CareGiverRequestDto;
import com.hinetics.caresync.dto.user.CareGiverRequestSendDto;
import com.hinetics.caresync.dto.user.UserSummaryDto;
import com.hinetics.caresync.entity.CareGiverRequest;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.UserRole;
import com.hinetics.caresync.repository.CareGiverRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareGiverRequestService {
    private final CareGiverAssignmentService assignmentService;
    private  final UserService userService;
    private final CareGiverRequestRepository requestRepository;

    public void sendRequest(String caregiverEmail,CareGiverRequestSendDto dto) {
        User fromUser = userService.getUserByEmail(caregiverEmail);

        User toUser =userService.getUserByEmail(dto.getPatientUserEmail());

        // Validation 1: Caregiver cannot send request to themselves
        if (fromUser.getEmail().equalsIgnoreCase(toUser.getEmail())) {
            throw new RuntimeException("Caregiver cannot send a request to themselves");
        }

        // Validation 2: Roles must match caregiver -> patient
        if (fromUser.getRole() != UserRole.CAREGIVER) {
            throw new RuntimeException("Sender must be a caregiver");
        }

        if (toUser.getRole() != UserRole.PATIENT) {
            throw new RuntimeException("Receiver must be a patient");
        }

        // Validation 3: Check if a request already exists
        boolean exists = requestRepository.existsByFromUserAndToUser(fromUser, toUser);
        if (exists) {
            throw new RuntimeException("A request already exists between caregiver and patient");
        }

        CareGiverRequest request = new CareGiverRequest();
        request.setFromUser(fromUser);
        request.setToUser(toUser);
        request.setRequestedPermission(dto.getRequestedPermission()); // set permission

        requestRepository.save(request);
    }

    public List<CareGiverRequestDto> getSentRequests(String caregiverEmail) {
        User caregiver = userService.getUserByEmail(caregiverEmail);

        return requestRepository.findByFromUser(caregiver)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<CareGiverRequestDto> getReceivedRequests(String patientEmail) {
        User patient = userService.getUserByEmail(patientEmail);

        return requestRepository.findByToUser(patient)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void acceptRequest(String patientEmail, Long requestId) {
        User patient = userService.getUserByEmail(patientEmail);

        CareGiverRequest request = requestRepository.findByIdAndToUser(requestId, patient)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User caregiver = request.getFromUser();

        assignmentService.assignCaregiverToPatient(
                caregiver,
                patient,
                request.getRequestedPermission()
        );

        requestRepository.delete(request);
    }

    public void rejectRequest(String patientEmail, Long requestId) {
        User patient = userService.getUserByEmail(patientEmail);

        CareGiverRequest request = requestRepository.findByIdAndToUser(requestId, patient)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        requestRepository.delete(request);
    }

    private CareGiverRequestDto mapToDto(CareGiverRequest request) {
        return new CareGiverRequestDto(
                request.getId(),
                new UserSummaryDto(
                        request.getFromUser().getId(),
                        request.getFromUser().getEmail(),
                        request.getFromUser().getName(),
                        request.getFromUser().getRole()
                ),
                new UserSummaryDto(
                        request.getToUser().getId(),
                        request.getToUser().getEmail(),
                        request.getToUser().getName(),
                        request.getToUser().getRole()
                ),
                request.getRequestedAt(),
                request.getRequestedPermission()
        );
    }

}
