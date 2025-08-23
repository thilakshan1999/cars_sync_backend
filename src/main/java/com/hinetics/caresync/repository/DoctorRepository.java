package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Doctor;
import com.hinetics.caresync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    Optional<Doctor> findByNameIgnoreCaseAndUser(String name, User user);
    Optional<Doctor> findByIdAndUser(Long id, User user);

}
