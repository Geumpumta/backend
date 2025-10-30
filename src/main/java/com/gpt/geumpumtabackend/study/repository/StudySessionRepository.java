package com.gpt.geumpumtabackend.study.repository;

import com.gpt.geumpumtabackend.study.domain.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Optional<StudySession> findByIdAndUser_Id(Long id, Long userId);


    // 날짜가 오늘이고, userId와 일치하고, endTime이 null이 아닌 것
    @Query(value = "SELECT COALESCE(SUM(s.total_millis), 0) " +
            "FROM study_session s " +
            "WHERE s.user_id = :userId " +
            "AND s.end_time BETWEEN :startOfDay AND :endOfDay", nativeQuery = true)
    Long sumCompletedStudySessionByUserId(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
