package com.hinetics.caresync.service;


import com.hinetics.caresync.dto.VitalMeasurementDto;
import com.hinetics.caresync.dto.analysed.DoctorAnalysisDto;
import com.hinetics.caresync.dto.analysed.VitalAnalysisDto;
import com.hinetics.caresync.dto.extracted.VitalExtractedDto;
import com.hinetics.caresync.entity.*;
import com.hinetics.caresync.enums.EntityStatus;
import com.hinetics.caresync.repository.DocumentRepository;
import com.hinetics.caresync.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VitalService {
    private final VitalRepository vitalRepository;
    private final DocumentRepository documentRepository;

    public void mergeDuplicateVitals(String vitalName, User user) {
        List<Vital> duplicates = vitalRepository.findAllByNameIgnoreCaseAndUser(vitalName, user);

        if (duplicates.size() <= 1) return;

        Vital mainVital = duplicates.get(0);

        for (int i = 1; i < duplicates.size(); i++) {
            Vital duplicate = duplicates.get(i);

            // Merge fields
            mainVital.setRemindDuration(nonNullOrDefault(mainVital.getRemindDuration(), duplicate.getRemindDuration()));
            mainVital.setStartDateTime(nonNullOrDefault(mainVital.getStartDateTime(), duplicate.getStartDateTime()));
            mainVital.setUnit(nonEmptyOrDefault(mainVital.getUnit(), duplicate.getUnit()));

            // Move linked documents
            List<Document> linkedDocs = documentRepository.findAllByVitalId(duplicate.getId());
            for (Document doc : linkedDocs) {
                doc.getVitals().remove(duplicate);
                if (!doc.getVitals().contains(mainVital)) {
                    doc.getVitals().add(mainVital);
                }
                documentRepository.save(doc);
            }

            // Delete duplicate
            vitalRepository.delete(duplicate);
        }

        vitalRepository.save(mainVital);
    }


    public Optional<Vital> findByNameAndUser(String vitalName,User user) {
        return vitalRepository.findByNameIgnoreCaseAndUser(vitalName,user);
    }

    public List<Vital> processVitals(List<VitalAnalysisDto> dtoList,User user) {
        List<Vital> vitals = new ArrayList<>();

        for (VitalAnalysisDto vitalDto : dtoList) {
            if (vitalDto.getEntityStatus() == EntityStatus.NEW) {
                Vital vital = new Vital();
                vital.setName(vitalDto.getName());
                vital.setRemindDuration(vitalDto.getRemindDuration());
                vital.setStartDateTime(vitalDto.getStartDateTime());
                vital.setUnit(vitalDto.getUnit());
                vital.setUser(user);

                List<VitalMeasurement> measurements = vitalDto.getMeasurements().stream()
                        .map(dto -> {
                            VitalMeasurement vm = new VitalMeasurement();
                            vm.setDateTime(dto.getDateTime());
                            vm.setValue(dto.getValue());
                            return vm;
                        })
                        .collect(Collectors.toList());
                vital.setMeasurements(measurements);

                vitals.add(vital);

            } else if (vitalDto.getEntityStatus() == EntityStatus.UPDATED) {
                if (vitalDto.getId() != null) {
                    Vital existingVital = vitalRepository.findByIdAndUser(vitalDto.getId(), user)
                            .orElseThrow(() -> new IllegalArgumentException("Vital not found with id: " + vitalDto.getId()+" for this user"));

                    existingVital.setName(vitalDto.getName());
                    existingVital.setRemindDuration(vitalDto.getRemindDuration());
                    existingVital.setStartDateTime(vitalDto.getStartDateTime());
                    existingVital.setUnit(vitalDto.getUnit());

                    List<VitalMeasurement> updatedMeasurements = vitalDto.getMeasurements().stream()
                            .map(dto -> {
                                VitalMeasurement vm = new VitalMeasurement();
                                vm.setDateTime(dto.getDateTime());
                                vm.setValue(dto.getValue());
                                return vm;
                            })
                            .collect(Collectors.toList());

                    existingVital.setMeasurements(updatedMeasurements);

                    vitals.add(existingVital);
                }
            } else if (vitalDto.getEntityStatus() == EntityStatus.SAME) {
                if (vitalDto.getId() != null) {
                    Vital existingVital = vitalRepository.findByIdAndUser(vitalDto.getId(), user)
                            .orElseThrow(() -> new IllegalArgumentException("Vital not found with id: " + vitalDto.getId()+" for this user"));
                    vitals.add(existingVital);
                }
            }
        }

        return  vitals;
    }

    public List<VitalAnalysisDto> mapAll(List<VitalExtractedDto> extractedVitals, User user) {
        if (extractedVitals == null) return Collections.emptyList();

        return extractedVitals.stream()
                .map(dto -> mapToVitalAnalysisDTO(dto, user))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public VitalAnalysisDto mapToVitalAnalysisDTO(VitalExtractedDto extractedVital,User user) {
        if (extractedVital.getName() == null || extractedVital.getName().isEmpty()) {
            return null;
        }

        mergeDuplicateVitals(extractedVital.getName(),user);
        Optional<Vital> existingVitalOpt = findByNameAndUser(extractedVital.getName(),user);

        VitalAnalysisDto vitalDTO = new VitalAnalysisDto();
        vitalDTO.setName(extractedVital.getName());

        if (existingVitalOpt.isPresent()) {
            Vital existingVital = existingVitalOpt.get();
            vitalDTO.setId(existingVital.getId());
            vitalDTO.setRemindDuration(existingVital.getRemindDuration());
            vitalDTO.setStartDateTime(existingVital.getStartDateTime());

            boolean isUpdated = false;
            String extractedUnit = extractedVital.getUnit();
            if (extractedUnit != null && !extractedUnit.isEmpty() && !extractedUnit.equals(existingVital.getUnit())) {
                vitalDTO.setUnit(extractedUnit);  // update to new unit
                System.out.println("extractedUnit Updated");
                isUpdated = true;
            }else{
                vitalDTO.setUnit(existingVital.getUnit());
            }

            List<VitalMeasurementDto> updatedMeasurements = new ArrayList<>();
            if (existingVital.getMeasurements() != null) {
                for (VitalMeasurement measurementEntity : existingVital.getMeasurements()) {
                    VitalMeasurementDto dto = new VitalMeasurementDto();
                    dto.setDateTime(measurementEntity.getDateTime());
                    dto.setValue(measurementEntity.getValue());
                    updatedMeasurements.add(dto);
                }
            }

            if (extractedVital.getValue() != null && !extractedVital.getValue().isEmpty()) {
                LocalDateTime dateTime = extractedVital.getDateTime() != null ? extractedVital.getDateTime() : LocalDateTime.now();

                Optional<VitalMeasurementDto> existingMeasurementOpt = updatedMeasurements.stream()
                        .filter(m -> m.getDateTime().equals(dateTime))
                        .findFirst();

                if (existingMeasurementOpt.isPresent()) {
                    VitalMeasurementDto existingMeasurement = existingMeasurementOpt.get();
                    if (!extractedVital.getValue().equals(existingMeasurement.getValue())) {
                        existingMeasurement.setValue(extractedVital.getValue());
                        System.out.println("measurement Updated");
                        isUpdated = true;
                    }
                } else {
                    VitalMeasurementDto newMeasurement = new VitalMeasurementDto();
                    newMeasurement.setDateTime(dateTime);
                    newMeasurement.setValue(extractedVital.getValue());
                    updatedMeasurements.add(newMeasurement);
                    System.out.println("measurement2 Updated");
                    isUpdated = true;
                }
            }

            vitalDTO.setMeasurements(updatedMeasurements);
            vitalDTO.setEntityStatus(isUpdated ? EntityStatus.UPDATED : EntityStatus.SAME);
        }
        else {
            vitalDTO.setId(null);
            vitalDTO.setRemindDuration(null);

            LocalDateTime dateTime = extractedVital.getDateTime() != null ? extractedVital.getDateTime() : LocalDateTime.now();
            vitalDTO.setStartDateTime(extractedVital.getDateTime());

            vitalDTO.setUnit(extractedVital.getUnit());

            VitalMeasurementDto measurement = new VitalMeasurementDto();
            measurement.setDateTime(dateTime);
            measurement.setValue(extractedVital.getValue());

            List<VitalMeasurementDto> measurements = new ArrayList<>();
            measurements.add(measurement);

            vitalDTO.setMeasurements(measurements);
            vitalDTO.setEntityStatus(EntityStatus.NEW);
        }
        return  vitalDTO;
    }

    private String nonEmptyOrDefault(String mainValue, String duplicateValue) {
        if (mainValue != null && !mainValue.isEmpty()) return mainValue;
        return duplicateValue;
    }

    private <T> T nonNullOrDefault(T mainValue, T duplicateValue) {
        return mainValue != null ? mainValue : duplicateValue;
    }
}
