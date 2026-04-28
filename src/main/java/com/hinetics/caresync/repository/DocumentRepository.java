package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.Document;
import com.hinetics.caresync.entity.User;
import com.hinetics.caresync.enums.DocumentType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findByUser(User user);
    List<Document> findByUserAndDocumentType(User user, DocumentType documentType);
    List<Document> findByUser(User user, Sort sort);
    List<Document> findByUserAndDocumentType(User user, DocumentType documentType, Sort sort);
    Optional<Document> findByIdAndUserEmail(Long id, String email);

    @Query("SELECT d FROM Document d JOIN d.doctors doc WHERE doc.id = :doctorId")
    List<Document> findAllByDoctorId(@Param("doctorId") Long doctorId);

    @Query("SELECT d FROM Document d JOIN d.medicines m WHERE m.id = :medId")
    List<Document> findAllByMedId(@Param("medId") Long medId);

    @Query("SELECT d FROM Document d JOIN d.vitals v WHERE v.id = :vitalId")
    List<Document> findAllByVitalId(@Param("vitalId") Long vitalId);

    @Query("SELECT d FROM Document d JOIN d.appointments a WHERE a.id = :appointmentId")
    List<Document> findAllByAppointmentId(@Param("appointmentId") Long appointmentId);

    List<Document> findByUserIdInAndUpdatedTimeAfterOrderByUpdatedTimeAsc(List<Long> userIds, LocalDateTime lastSyncTime);
}
