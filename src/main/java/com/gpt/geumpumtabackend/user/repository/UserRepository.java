package com.gpt.geumpumtabackend.user.repository;

import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByProviderAndProviderIdAndDeletedAtIsNotNull(OAuth2Provider provider, String providerId);

    User findByProviderIdAndDeletedAtIsNull(String providerId);

    Optional<User> findByProviderAndProviderIdAndDeletedAtIsNull(OAuth2Provider provider, String providerId);

    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
}
