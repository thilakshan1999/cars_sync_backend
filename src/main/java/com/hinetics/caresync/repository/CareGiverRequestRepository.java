package com.hinetics.caresync.repository;

import com.hinetics.caresync.entity.CareGiverRequest;
import com.hinetics.caresync.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareGiverRequestRepository extends JpaRepository<CareGiverRequest,Long> {
    List<CareGiverRequest> findByFromUser(User fromUser);

    List<CareGiverRequest> findByToUser(User toUser);

    Optional<CareGiverRequest> findByIdAndToUser(Long id, User toUser);

    boolean existsByFromUserAndToUser(User fromUser, User toUser);
}
