package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Doctor;
import com.hinetics.caresync.entity.Med;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedRepository extends JpaRepository<Med,Long> {
    Optional<Med> findByNameIgnoreCase(String name);
}
