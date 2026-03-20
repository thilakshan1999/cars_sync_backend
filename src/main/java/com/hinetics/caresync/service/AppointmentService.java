package com.hinetics.caresync.service;

import com.hinetics.caresync.dto.analysed.AppointmentAnalysisDto;
import com.hinetics.caresync.dto.analysed.DoctorAnalysisDto;
import com.hinetics.caresync.dto.extracted.AppointmentExtractedDto;
import com.hinetics.caresync.dto.extracted.DoctorExtractedDto;
import com.hinetics.caresync.entity.Appointment;
import com.hinetics.caresync.entity.Doctor;
import com.hinetics.caresync.entity.Document;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.AppointmentType;
import com.hinetics.caresync.enums.EntityStatus;
import com.hinetics.caresync.enums.MedForm;
import com.hinetics.caresync.repository.AppointmentRepository;
import com.hinetics.caresync.repository.DoctorRepository;
import com.hinetics.caresync.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final DocumentRepository documentRepository;

    public void mergeDuplicateAppointments(LocalDateTime dateTime, String  doctorName, User user) {
        if (doctorName  == null || dateTime == null || user == null) {
            return;
        }

        List<Appointment> duplicates = appointmentRepository.findAllDuplicatesByDoctorName(doctorName, dateTime, user);

        if (duplicates.size() <= 1) return;

        Appointment main = duplicates.get(0);

        for (int i = 1; i < duplicates.size(); i++) {
            Appointment duplicate = duplicates.get(i);

            // Merge fields if missing
            main.setName(nonEmptyOrDefault(main.getName(), duplicate.getName()));
            main.setType(nonNullOrDefault(main.getType(), duplicate.getType()));

            // Move linked documents
            List<Document> linkedDocs = documentRepository.findAllByAppointmentId(duplicate.getId());
            for (Document doc : linkedDocs) {
                doc.getAppointments().remove(duplicate);
                if (!doc.getAppointments().contains(main)) {
                    doc.getAppointments().add(main);
                }
                documentRepository.save(doc);
            }

            // Delete duplicate appointment
            appointmentRepository.delete(duplicate);
        }

        appointmentRepository.save(main);
    }

    public Optional<Appointment> findByDateTimeAndDoctorNameAndUser(
            LocalDateTime dateTime,
            String doctorName,
            User user
    ) {
        if (dateTime == null || doctorName == null || doctorName.isBlank() || user == null) {
            return Optional.empty();
        }
        return appointmentRepository.findByDateTimeAndDoctorNameAndUser(dateTime, doctorName, user);
    }


    public List<Appointment> processAppointment(List<AppointmentAnalysisDto> dtoList, List<Doctor> processedDoctors,User user) {
        List<Appointment> appointments = new ArrayList<>();

        for (AppointmentAnalysisDto appointmentDto : dtoList) {
            if (appointmentDto.getEntityStatus() == EntityStatus.NEW) {
                Appointment appointment = new Appointment();
                appointment.setName(appointmentDto.getName());
                appointment.setType(appointmentDto.getType());
                appointment.setUser(user);
                // Assuming you have doctor entity to set
                DoctorAnalysisDto doctorDto = appointmentDto.getDoctor();
                 if (doctorDto!=null&& processedDoctors != null){
                    Doctor doctor =processedDoctors.stream()
                            .filter(d -> d.getName().equalsIgnoreCase(doctorDto.getName()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Doctor not processed: " + doctorDto.getName()));

//                            doctorRepository.findByNameIgnoreCase(doctorDto.getName())
//                            .orElseThrow(() -> new IllegalArgumentException("Doctor not found with name: " + doctorDto.getName()));

                     appointment.setDoctor(doctor);
                }
                appointment.setAppointmentDateTime(appointmentDto.getAppointmentDateTime());

                appointments.add(appointment);

            } else if (appointmentDto.getEntityStatus() == EntityStatus.UPDATED) {
                if (appointmentDto.getId() != null) {
                    Appointment existingAppointment = appointmentRepository
                            .findByIdAndUser(appointmentDto.getId(), user) // only for this user
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Appointment not found with id: " + appointmentDto.getId() + " for this user"
                            ));

                    existingAppointment.setName(appointmentDto.getName());
                    existingAppointment.setType(appointmentDto.getType());

                    DoctorAnalysisDto doctorDto = appointmentDto.getDoctor();
                    if (doctorDto != null&& processedDoctors != null) {
                        Doctor doctor = processedDoctors.stream()
                                .filter(d -> d.getName().equalsIgnoreCase(doctorDto.getName()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Doctor not processed: " + doctorDto.getName()));

//                                doctorRepository.findByNameIgnoreCase(doctorDto.getName())
//                                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with name: " + doctorDto.getName()));
                        existingAppointment.setDoctor(doctor);
                    }
                    existingAppointment.setAppointmentDateTime(appointmentDto.getAppointmentDateTime());

                    appointments.add(existingAppointment);
                }
            } else if (appointmentDto.getEntityStatus() == EntityStatus.SAME) {
                if (appointmentDto.getId() != null) {
                    Appointment existingAppointment = appointmentRepository
                            .findByIdAndUser(appointmentDto.getId(), user) // only for this user
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Appointment not found with id: " + appointmentDto.getId() + " for this user"
                            ));
                    appointments.add(existingAppointment);
                }
            }
        }

        return appointments;
    }

    public List<AppointmentAnalysisDto> mapAll(List<AppointmentExtractedDto> extractedAppointments, List<DoctorAnalysisDto> extractedDoctors, User user) {
        return extractedAppointments.stream()
                .map(dto -> mapToAppointmentAnalysisDto(dto, extractedDoctors,user))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AppointmentAnalysisDto mapToAppointmentAnalysisDto(AppointmentExtractedDto extractedAppointment,List<DoctorAnalysisDto> extractedDoctors,User user){
        if (extractedAppointment.getAppointmentDateTime() == null  ) {
            return null;
        }

        AppointmentAnalysisDto appointmentAnalysisDto = new AppointmentAnalysisDto();
        appointmentAnalysisDto.setAppointmentDateTime(extractedAppointment.getAppointmentDateTime());

        String doctorName = extractedAppointment.getDoctorName();

        if (doctorName != null && !doctorName.isEmpty()){
            mergeDuplicateAppointments(extractedAppointment.getAppointmentDateTime(),extractedAppointment.getDoctorName(),user);
            Optional<Appointment> existingAppointmentOpt = findByDateTimeAndDoctorNameAndUser(extractedAppointment.getAppointmentDateTime(),extractedAppointment.getDoctorName(),user);

            DoctorAnalysisDto doctorDto = extractedDoctors.stream()
                    .filter(doc -> doc.getName() != null &&
                            doc.getName().equalsIgnoreCase(extractedAppointment.getDoctorName()))
                    .findFirst()
                    .orElse(null);

            appointmentAnalysisDto.setDoctor(doctorDto);
            if(existingAppointmentOpt.isPresent()){
                Appointment existingAppointment = existingAppointmentOpt.get();
                appointmentAnalysisDto.setId(existingAppointment.getId());

                boolean isUpdated = false;

                //Name
                String extractedName = extractedAppointment.getName();
                if (extractedName != null && !extractedName.isEmpty() && !extractedName.equals(existingAppointment.getName())) {
                    appointmentAnalysisDto.setName(extractedName);  // update to new unit
                    isUpdated = true;
                }else{
                    appointmentAnalysisDto.setName(existingAppointment.getName());
                }

                AppointmentType extractedAppointmentType = extractedAppointment.getType();

                if(extractedAppointmentType != existingAppointment.getType()){
                    appointmentAnalysisDto.setType(extractedAppointmentType);
                    isUpdated = true;
                }else {
                    appointmentAnalysisDto.setType(existingAppointment.getType());
                }

                appointmentAnalysisDto.setEntityStatus(isUpdated ? EntityStatus.UPDATED : EntityStatus.SAME);

            }else {
                appointmentAnalysisDto.setId(null);
                appointmentAnalysisDto.setName(extractedAppointment.getName());
                appointmentAnalysisDto.setType(extractedAppointment.getType());
                appointmentAnalysisDto.setEntityStatus(EntityStatus.NEW);
            }
        }else{
            appointmentAnalysisDto.setId(null);
            appointmentAnalysisDto.setName(extractedAppointment.getName());
            appointmentAnalysisDto.setType(extractedAppointment.getType());
            appointmentAnalysisDto.setDoctor(null);
            appointmentAnalysisDto.setEntityStatus(EntityStatus.NEW);
        }

        return  appointmentAnalysisDto;
    }

    private String nonEmptyOrDefault(String mainValue, String duplicateValue) {
        return (mainValue != null && !mainValue.isEmpty()) ? mainValue : duplicateValue;
    }

    private <T> T nonNullOrDefault(T mainValue, T duplicateValue) {
        return mainValue != null ? mainValue : duplicateValue;
    }
}
