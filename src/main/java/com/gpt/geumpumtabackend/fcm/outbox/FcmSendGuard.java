package com.gpt.geumpumtabackend.fcm.outbox;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.gpt.geumpumtabackend.fcm.domain.NotificationOutbox;
import com.gpt.geumpumtabackend.fcm.service.FcmService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.stereotype.Component;

@Component
public class FcmSendGuard {

    private final FcmService fcmService;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;

    public FcmSendGuard(
            FcmService fcmService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry
    ) {
        this.fcmService = fcmService;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("fcmSend");
        this.rateLimiter = rateLimiterRegistry.rateLimiter("fcmSend");
    }

    public String send(NotificationOutbox outbox) throws FirebaseMessagingException {
        if(!rateLimiter.acquirePermission()) {
            throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        }

        try {
            return circuitBreaker.executeCallable(()-> fcmService.sendOutbox(outbox));
        } catch (FirebaseMessagingException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
