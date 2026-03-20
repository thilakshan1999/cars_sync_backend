package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Doctor;
import com.hinetics.caresync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    List<Doctor> findAllByNameIgnoreCaseAndUser(String name, User user);
    Optional<Doctor> findByNameIgnoreCaseAndUser(String name, User user);
    Optional<Doctor> findByIdAndUser(Long id, User user);

}
