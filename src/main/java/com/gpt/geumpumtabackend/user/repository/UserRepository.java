package com.gpt.geumpumtabackend.user.repository;

import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByProviderAndProviderIdAndDeletedAtIsNotNull(OAuth2Provider provider, String providerId);

    User findByProviderIdAndDeletedAtIsNull(String providerId);

    Optional<User> findByProviderAndProviderIdAndDeletedAtIsNull(OAuth2Provider provider, String providerId);

    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);

    boolean existsByStudentId(String studentId);

    boolean existsBySchoolEmail(String schoolEmail);

    Optional<User> findByFcmToken(String fcmToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
