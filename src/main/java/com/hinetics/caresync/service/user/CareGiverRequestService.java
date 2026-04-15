package com.hinetics.caresync.service.user;

import com.hinetics.caresync.dto.user.CareGiverRequestDto;
import com.hinetics.caresync.dto.user.CaregiverRequestSendDto;
import com.hinetics.caresync.dto.user.UserSummaryDto;
import com.hinetics.caresync.entity.CareGiverRequest;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.CareGiverPermission;
import com.hinetics.caresync.enums.UserRole;
import com.hinetics.caresync.repository.CareGiverRequestRepository;
import com.hinetics.caresync.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareGiverRequestService {
    private final CareGiverAssignmentService assignmentService;
    private  final UserService userService;
    private final CareGiverRequestRepository requestRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public void sendRequest(String senderEmail, CaregiverRequestSendDto dto) {
        User fromUser = userService.getUserByEmail(senderEmail);

        User toUser =userService.getUserByEmail(dto.getReceiveUserEmail());

        // Validation 1: Caregiver cannot send request to themselves
        if (fromUser.getEmail().equalsIgnoreCase(toUser.getEmail())) {
            throw new RuntimeException("User cannot send a request to themselves");
        }

        // Validation 2: Roles must match caregiver -> patient or patient -> caregiver
        UserRole fromRole = fromUser.getRole();
        UserRole toRole = toUser.getRole();

        boolean validPair =
                (fromRole == UserRole.CAREGIVER && toRole == UserRole.PATIENT) ||
                        (fromRole == UserRole.PATIENT && toRole == UserRole.CAREGIVER);

        if (!validPair) {
            throw new RuntimeException("Requests can only be sent between caregivers and patients");
        }

        // Validation 3: Check if a request already exists
        boolean existsForward = requestRepository.existsByFromUserAndToUser(fromUser, toUser);
        boolean existsReverse = requestRepository.existsByFromUserAndToUser(toUser, fromUser);


        if (existsForward) {
            throw new RuntimeException("You have already sent a request to this user");
        }

        if (existsReverse) {
            throw new RuntimeException("This user has already sent you a request");
        }

        // Validation 4: Check if already assigned
        boolean alreadyAssigned = (fromRole == UserRole.PATIENT)
                ? assignmentService.isAssignedToPatient(toUser, fromUser)
                : assignmentService.isAssignedToPatient(fromUser, toUser);

        if (alreadyAssigned) {
            throw new RuntimeException("Users are already connected");
        }

        CareGiverRequest request = new CareGiverRequest();
        request.setFromUser(fromUser);
        request.setToUser(toUser);
        request.setRequestedPermission(dto.getRequestedPermission()); // set permission

        requestRepository.save(request);
    }

    public List<CareGiverRequestDto> getSentRequests(String senderEmail) {
        User sender = userService.getUserByEmail(senderEmail);

        return requestRepository.findByFromUser(sender)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<CareGiverRequestDto> getReceivedRequests(String receiverEmail) {
        User receiver = userService.getUserByEmail(receiverEmail);

        return requestRepository.findByToUser(receiver)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void acceptRequest(String receiverEmail, Long requestId) {
        User receiver = userService.getUserByEmail(receiverEmail);
        CareGiverRequest request = requestRepository.findByIdAndToUser(requestId, receiver)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        boolean isPatient = receiver.getRole() == UserRole.PATIENT;
        User patient = isPatient ? receiver : request.getFromUser();
        User caregiver = isPatient ? request.getFromUser() : receiver;

        assignmentService.assignCaregiverToPatient(caregiver, patient, request.getRequestedPermission());
        requestRepository.delete(request);
    }

    public void rejectRequest(String receiverEmail, Long requestId) {
        User receiver = userService.getUserByEmail(receiverEmail);

        CareGiverRequest request = requestRepository.findByIdAndToUser(requestId, receiver)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        requestRepository.delete(request);
    }

    private CareGiverRequestDto mapToDto(CareGiverRequest request) {
        return new CareGiverRequestDto(
                request.getId(),
                new UserSummaryDto(
                        request.getFromUser().getId(),
                        request.getFromUser().getEmail(),
                        request.getFromUser().getSystemEmail(),
                        request.getFromUser().getName(),
                        request.getFromUser().getRole()
                ),
                new UserSummaryDto(
                        request.getToUser().getId(),
                        request.getToUser().getEmail(),
                        request.getToUser().getSystemEmail(),
                        request.getToUser().getName(),
                        request.getToUser().getRole()
                ),
                request.getRequestedAt(),
                request.getRequestedPermission()
        );
    }

    public String generateQrToken(String patientEmail, String requestedPermission) {
        User patient = userService.getUserByEmail(patientEmail);

        if (patient.getRole() != UserRole.PATIENT) {
            throw new RuntimeException("Only patients can generate QR token");
        }

        return jwtTokenUtil.generateQrToken(patient.getEmail(), requestedPermission);
    }

    public void linkAccountViaQr(String caregiverEmail, String qrToken) {
        User caregiver = userService.getUserByEmail(caregiverEmail);

        if (caregiver.getRole() != UserRole.CAREGIVER) {
            throw new RuntimeException("Only caregivers can link accounts via QR");
        }

        // Validate JWT & parse claims
        if (!jwtTokenUtil.validateToken(qrToken)) {
            throw new RuntimeException("Invalid or expired QR token");
        }

        String patientEmail = jwtTokenUtil.getEmailFromToken(qrToken);
        String permissionStr  = jwtTokenUtil.getPermissionFromToken(qrToken);

        CareGiverPermission permission = CareGiverPermission.fromString(permissionStr);
        User patient = userService.getUserByEmail(patientEmail);

        // Assign caregiver to patient
        assignmentService.assignCaregiverToPatient(caregiver, patient, permission);
    }
}
