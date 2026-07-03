package com.gpt.geumpumtabackend.fcm.repository;

import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    boolean existsByEventKey(String eventKey);

    @Query("""
    SELECT n FROM NotificationOutbox n WHERE  n.status IN :statuses
    AND (n.nextRetryAt IS NULL OR n.nextRetryAt<= :now) 
    ORDER BY n.id ASC
    """)
    List<NotificationOutbox> findDueOutboxes(@Param("statuses") Collection<NotificationOutboxStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationOutbox  n WHERE n.id = :id")
    Optional<NotificationOutbox> findByIdForUpdate(@Param("id") Long id);
}



