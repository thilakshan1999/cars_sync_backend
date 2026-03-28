package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.UploadTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadTaskRepository extends JpaRepository<UploadTask, Long> {
    List<UploadTask> findByPatientIdOrderByIdDesc(Long patientId);
}
