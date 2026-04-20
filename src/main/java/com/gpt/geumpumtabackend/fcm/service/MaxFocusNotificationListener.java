package com.gpt.geumpumtabackend.fcm.service;

import com.gpt.geumpumtabackend.study.event.MaxFocusSessionEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MaxFocusNotificationListener {

    private final FcmService fcmService;

    @Async("fcmExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MaxFocusSessionEndedEvent event) {
        try {
            fcmService.sendMaxFocusNotification(event.userId(), event.maxFocusHours());
        } catch (Exception e) {
            log.error("[MAX_FOCUS_LISTENER] unexpected error. userId={}", event.userId(), e);
        }
    }
}
