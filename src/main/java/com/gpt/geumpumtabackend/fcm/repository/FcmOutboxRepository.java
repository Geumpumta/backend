package com.gpt.geumpumtabackend.fcm.repository;

import com.gpt.geumpumtabackend.fcm.domain.FcmNotificationOutbox;
import com.gpt.geumpumtabackend.fcm.domain.FcmOutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmOutboxRepository extends JpaRepository<FcmNotificationOutbox, Long> {

    List<FcmNotificationOutbox> findAllByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
            FcmOutboxStatus status,
            LocalDateTime now
    );
}
