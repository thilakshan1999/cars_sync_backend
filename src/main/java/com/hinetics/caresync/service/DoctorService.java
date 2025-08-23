package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.analysed.DoctorAnalysisDto;
import com.hinetics.caresync.dto.extracted.DoctorExtractedDto;
import com.hinetics.caresync.entity.Doctor;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.EntityStatus;
import com.hinetics.caresync.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final DoctorRepository doctorRepository;

    public Optional<Doctor> findByNameAndUser(String doctorName, User user) {
        return doctorRepository.findByNameIgnoreCaseAndUser(doctorName, user);
    }

    public List<Doctor> processDoctors(List<DoctorAnalysisDto> dtoList,User user) {
        List<Doctor> doctors = new ArrayList<>();

        for (DoctorAnalysisDto doctorDto : dtoList) {
            if (doctorDto.getEntityStatus() == EntityStatus.NEW) {
                Doctor newDoctor = new Doctor();
                newDoctor.setName(doctorDto.getName());
                newDoctor.setSpecialization(doctorDto.getSpecialization());
                newDoctor.setPhoneNumber(doctorDto.getPhoneNumber());
                newDoctor.setEmail(doctorDto.getEmail());
                newDoctor.setAddress(doctorDto.getAddress());
                newDoctor.setUser(user);
                doctors.add(newDoctor);

            } else if (doctorDto.getEntityStatus() == EntityStatus.UPDATED) {
                if (doctorDto.getId() != null) {
                    Doctor existingDoctor = doctorRepository.findByIdAndUser(doctorDto.getId(),user)
                            .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorDto.getId()+" for this user"));
                    existingDoctor.setName(doctorDto.getName());
                    existingDoctor.setSpecialization(doctorDto.getSpecialization());
                    existingDoctor.setPhoneNumber(doctorDto.getPhoneNumber());
                    existingDoctor.setEmail(doctorDto.getEmail());
                    existingDoctor.setAddress(doctorDto.getAddress());
                    doctors.add(existingDoctor);
                }

            } else if (doctorDto.getEntityStatus() == EntityStatus.SAME) {
                if (doctorDto.getId() != null) {
                    Doctor existingDoctor = doctorRepository.findByIdAndUser(doctorDto.getId(),user)
                            .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorDto.getId()+" for this user"));
                    doctors.add(existingDoctor);
                }
            }
        }
        return doctors;
    }

    public List<DoctorAnalysisDto> mapAll(List<DoctorExtractedDto> extractedDoctors, User user) {
        return extractedDoctors.stream()
                .map(dto -> mapToDoctorAnalysisDto(dto, user))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private DoctorAnalysisDto mapToDoctorAnalysisDto(DoctorExtractedDto extractedDoctor,User user) {
        if (extractedDoctor.getName() == null || extractedDoctor.getName().isEmpty()) {
            return null;
        }

        Optional<Doctor> existingDoctorOpt = findByNameAndUser(extractedDoctor.getName(),user);

        DoctorAnalysisDto doctorDto = new DoctorAnalysisDto();
        doctorDto.setName(extractedDoctor.getName());

        if (existingDoctorOpt.isPresent()) {
            Doctor existingDoctor = existingDoctorOpt.get();
            doctorDto.setId(existingDoctor.getId());

            boolean isUpdated = false;

            // Compare specialization
            if (isDifferent(existingDoctor.getSpecialization(), extractedDoctor.getSpecialization())) {
                isUpdated = true;
            }
            doctorDto.setSpecialization(
                    nonEmptyOrDefault(extractedDoctor.getSpecialization(), existingDoctor.getSpecialization())
            );

            // Compare phoneNumber
            if (isDifferent(existingDoctor.getPhoneNumber(), extractedDoctor.getPhoneNumber())) {
                isUpdated = true;
            }
            doctorDto.setPhoneNumber(
                    nonEmptyOrDefault(extractedDoctor.getPhoneNumber(), existingDoctor.getPhoneNumber())
            );

            // Compare email
            if (isDifferent(existingDoctor.getEmail(), extractedDoctor.getEmail())) {
                isUpdated = true;
            }
            doctorDto.setEmail(
                    nonEmptyOrDefault(extractedDoctor.getEmail(), existingDoctor.getEmail())
            );

            // Compare address
            if (isDifferent(existingDoctor.getAddress(), extractedDoctor.getAddress())) {
                isUpdated = true;
            }
            doctorDto.setAddress(
                    nonEmptyOrDefault(extractedDoctor.getAddress(), existingDoctor.getAddress())
            );

            doctorDto.setEntityStatus(isUpdated ? EntityStatus.UPDATED : EntityStatus.SAME);

        }else {
            doctorDto.setId(null);
            doctorDto.setSpecialization(extractedDoctor.getSpecialization());
            doctorDto.setPhoneNumber(extractedDoctor.getPhoneNumber());
            doctorDto.setEmail(extractedDoctor.getEmail());
            doctorDto.setAddress(extractedDoctor.getAddress());
            doctorDto.setEntityStatus(EntityStatus.NEW);
        }
        return doctorDto;
    }

    private boolean isDifferent(String existingValue, String extractedValue) {
        if (extractedValue == null || extractedValue.isEmpty()) {
            // extracted value empty, ignore difference
            return false;
        }
        return !extractedValue.equals(existingValue);
    }

    private String nonEmptyOrDefault(String extractedValue, String existingValue) {
        if (extractedValue != null && !extractedValue.isEmpty()) {
            return extractedValue;
        }
        return existingValue;
    }
}
