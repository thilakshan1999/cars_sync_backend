package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Vital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VitalRepository extends JpaRepository<Vital, Long> {
    Optional<Vital> findByNameIgnoreCase(String name);
}
