package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.CareGiverAssignment;
import com.hinetics.caresync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareGiverAssignmentRepository extends JpaRepository<CareGiverAssignment, Long> {
    List<CareGiverAssignment> findByPatientId(Long patientId);
    List<CareGiverAssignment> findByCaregiverId(Long caregiverId);
    boolean existsByCaregiverAndPatient(User caregiver, User patient);
    Optional<CareGiverAssignment> findByCaregiverAndPatient(User caregiver, User patient);
}
