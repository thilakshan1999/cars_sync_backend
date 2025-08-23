package com.hinetics.caresync.service;


import com.hinetics.caresync.dto.analysed.DoctorAnalysisDto;
import com.hinetics.caresync.dto.analysed.MedAnalysisDto;
import com.hinetics.caresync.dto.extracted.MedExtractedDto;
import com.hinetics.caresync.entity.Doctor;
import com.hinetics.caresync.entity.Med;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.EntityStatus;
import com.hinetics.caresync.enums.IntakeInstruction;
import com.hinetics.caresync.enums.MedForm;
import com.hinetics.caresync.repository.MedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedService {
    private final MedRepository medRepository;

    public Optional<Med> findByNameAndUser(String medName,User user){return medRepository.findByNameIgnoreCaseAndUser(medName,user);}

    public List<Med> processMeds(List<MedAnalysisDto> dtoList,User user) {
        List<Med> meds = new ArrayList<>();

        for (MedAnalysisDto medDto : dtoList) {
            if (medDto.getEntityStatus() == EntityStatus.NEW) {
                Med med = new Med();
                med.setName(medDto.getName());
                med.setMedForm(medDto.getMedForm());
                med.setHealthCondition(medDto.getHealthCondition());
                med.setIntakeInterval(medDto.getIntakeInterval());
                med.setStartDate(medDto.getStartDate());
                med.setEndDate(medDto.getEndDate());
                med.setDosage(medDto.getDosage());
                med.setInstruction(medDto.getInstruction());
                med.setStock(med.getStock());
                med.setReminderLimit(med.getReminderLimit());
                med.setUser(user);

                meds.add(med);
            } else if (medDto.getEntityStatus() == EntityStatus.UPDATED) {
                if (medDto.getId() != null) {
                    Med existingMed = medRepository.findByIdAndUser(medDto.getId(), user)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Med not found with id: " + medDto.getId() + " for this user"
                            ));

                    existingMed.setName(medDto.getName());
                    existingMed.setMedForm(medDto.getMedForm());
                    existingMed.setHealthCondition(medDto.getHealthCondition());
                    existingMed.setIntakeInterval(medDto.getIntakeInterval());
                    existingMed.setStartDate(medDto.getStartDate());
                    existingMed.setEndDate(medDto.getEndDate());
                    existingMed.setDosage(medDto.getDosage());
                    existingMed.setInstruction(medDto.getInstruction());
                    existingMed.setStock(medDto.getStock());
                    existingMed.setReminderLimit(medDto.getReminderLimit());

                    meds.add(existingMed);
                }
            } else if (medDto.getEntityStatus() == EntityStatus.SAME) {
                if (medDto.getId() != null) {
                    Med existingMed = medRepository.findByIdAndUser(medDto.getId(), user)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Med not found with id: " + medDto.getId() + " for this user"
                            ));
                    meds.add(existingMed);
                }
            }
        }


        return  meds;
    }

    public List<MedAnalysisDto> mapAll(List<MedExtractedDto> extractedMeds, User user) {
        return extractedMeds.stream()
                .map(dto -> mapToMedAnalysisDto(dto, user))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MedAnalysisDto mapToMedAnalysisDto(MedExtractedDto extractedMed,User user) {
        if (extractedMed.getMedName() == null || extractedMed.getMedName().isEmpty()) {
            return null;
        }

        Optional<Med> existingMedOpt = findByNameAndUser(extractedMed.getMedName(),user);

        MedAnalysisDto medDto = new MedAnalysisDto();
        medDto.setName(extractedMed.getMedName());


        if (existingMedOpt.isPresent()) {
            Med existingMed = existingMedOpt.get();
            medDto.setId(existingMed.getId());
            medDto.setStock(existingMed.getStock());
            medDto.setReminderLimit(existingMed.getReminderLimit());

            boolean isUpdated = false;

            //medForm
            MedForm extractedMedForm = extractedMed.getMedForm();

            if(extractedMedForm != existingMed.getMedForm()){
                medDto.setMedForm(extractedMedForm);
                System.out.println("Med Form Updated");
                isUpdated = true;
            }else {
                medDto.setMedForm(existingMed.getMedForm());
            }

            //healthCondition
            if (isDifferent(existingMed.getHealthCondition(), existingMed.getHealthCondition())) {
                isUpdated = true;
                System.out.println("Health condition Updated");
            }
            medDto.setHealthCondition(
                    nonEmptyOrDefault(existingMed.getHealthCondition(), existingMed.getHealthCondition())
            );

            //intakeInterval
            Duration extractedInterval = extractedMed.getIntakeInterval();

            if(extractedInterval!=null && !extractedInterval.equals(existingMed.getIntakeInterval()) ){
                medDto.setIntakeInterval(extractedInterval);
                System.out.println("Interval Updated");
                isUpdated = true;
            }else {
                medDto.setIntakeInterval(existingMed.getIntakeInterval());
            }

            //startDate
            LocalDateTime extractedLocalStartDate = extractedMed.getStartDate();

            if(extractedLocalStartDate!=null && extractedLocalStartDate != existingMed.getStartDate()){
                medDto.setStartDate(extractedLocalStartDate);
                System.out.println("Start date Updated");
                isUpdated = true;
            }else {
                medDto.setStartDate(existingMed.getStartDate());
            }

            //EndDate
            LocalDate extractedLocalEndDate = extractedMed.getEndDate();

            if(extractedLocalEndDate!=null && extractedLocalEndDate != existingMed.getEndDate()){
                medDto.setEndDate(extractedLocalEndDate);
                System.out.println("End date Updated");
                isUpdated = true;
            }else {
                medDto.setEndDate(existingMed.getEndDate());
            }

            //Dosage
            if (isDifferent(existingMed.getDosage(), existingMed.getDosage())) {
                System.out.println("Dosage Updated");
                isUpdated = true;
            }
            medDto.setDosage(
                    nonEmptyOrDefault(existingMed.getDosage(), existingMed.getDosage())
            );

            //instruction
            IntakeInstruction extractedInstruction = extractedMed.getInstruction();

            if( extractedInstruction != existingMed.getInstruction()){

                medDto.setInstruction(extractedInstruction);
                System.out.println("Instruction Updated");
                isUpdated = true;
            }else {
                medDto.setInstruction(existingMed.getInstruction());
            }

            medDto.setEntityStatus(isUpdated ? EntityStatus.UPDATED : EntityStatus.SAME);

        }else{
            medDto.setId(null);
            medDto.setMedForm(extractedMed.getMedForm());
            medDto.setHealthCondition(extractedMed.getHealthCondition());
            medDto.setIntakeInterval(extractedMed.getIntakeInterval());
            LocalDateTime extractedLocalStartDate = extractedMed.getStartDate() != null ? extractedMed.getStartDate() : LocalDateTime.now();
            medDto.setStartDate(extractedLocalStartDate);
            medDto.setEndDate(extractedMed.getEndDate());
            medDto.setDosage(extractedMed.getDosage());
            medDto.setStock(null);
            medDto.setReminderLimit(null);
            medDto.setInstruction(extractedMed.getInstruction());
            medDto.setEntityStatus(EntityStatus.NEW);
        }

        return  medDto;
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
