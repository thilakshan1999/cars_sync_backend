package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Appointment;
import com.hinetics.caresync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("""
    SELECT a 
    FROM Appointment a 
    JOIN a.doctor d 
    WHERE a.appointmentDateTime = :dateTime
    AND LOWER(d.name) = LOWER(:doctorName)
    AND a.user = :user
""")
    Optional<Appointment> findByDateTimeAndDoctorNameAndUser(
            @Param("dateTime") LocalDateTime dateTime,
            @Param("doctorName") String doctorName,
            @Param("user") User user
    );
    Optional<Appointment> findByIdAndUser(Long id, User user);

}
