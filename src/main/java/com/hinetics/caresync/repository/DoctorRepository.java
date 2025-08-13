package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    Optional<Doctor> findByNameIgnoreCase(String name);
}
