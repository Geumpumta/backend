package com.gpt.geumpumtabackend.global.oauth.service;



import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2UserInfo;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. registrationId 가져오기 (third-party id)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2. 유저 정보(attributes) 가져오기
        Map<String, Object> oAuth2UserAttributes = loadAttributes(userRequest, registrationId);

        // 3. userNameAttributeName 가져오기
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        String providerId = oAuth2UserAttributes.get(userNameAttributeName).toString();

        // 4. 유저 정보 dto 생성
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of(registrationId, oAuth2UserAttributes);

        // 5. 회원가입 및 로그인
        User user = getOrSave(oAuth2UserInfo, registrationId, providerId);

        log.info("Access Token: {}", userRequest.getAccessToken().getTokenValue());

        // 추가적인 토큰 정보 로깅
        log.info("Token Type: {}", userRequest.getAccessToken().getTokenType());
        log.info("Scopes: {}", userRequest.getAccessToken().getScopes());
        log.info("Expires At: {}", userRequest.getAccessToken().getExpiresAt());

        // 클라이언트 등록 정보 로깅
        log.info("Registration ID: {}", userRequest.getClientRegistration().getRegistrationId());

        return new OAuth2UserPrincipal(user, oAuth2UserAttributes, userNameAttributeName);


    }

    private Map<String, Object> loadAttributes(OAuth2UserRequest userRequest, String registrationId) {
        if(registrationId.equals("apple")) {
            return loadAppleAttributes(userRequest);
        }
        return super.loadUser(userRequest).getAttributes();
    }

    private Map<String, Object> loadAppleAttributes(OAuth2UserRequest userRequest) {
        Object idTokenObj = userRequest.getAdditionalParameters().get("id_token");
        if (idTokenObj == null) {
            throw new OAuth2AuthenticationException("Apple id_token is missing in additionalParameters");
        }

        String idToken = idTokenObj.toString();

        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // claims.getClaims()는 Map<String, Object>를 반환
            Map<String, Object> attributes = new HashMap<>(claims.getClaims());

            // 로그로 한 번 확인해보고 싶으면:
            log.info("Apple id_token claims: {}", attributes);

            return attributes;
        } catch (ParseException e) {
            throw new OAuth2AuthenticationException("Failed to parse Apple id_token");
        }
    }

    private User getOrSave(OAuth2UserInfo oAuth2UserInfo, String registrationId, String providerId) {

        OAuth2Provider provider = resolveProvider(registrationId);

        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createNewUser(oAuth2UserInfo, provider, providerId));
    }

    private OAuth2Provider resolveProvider(String registrationId) {
        return switch (registrationId) {
            case "google" -> OAuth2Provider.GOOGLE;
            case "kakao"  -> OAuth2Provider.KAKAO;
            case "apple"  -> OAuth2Provider.APPLE;
            default       -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private User createNewUser(OAuth2UserInfo info, OAuth2Provider provider, String providerId) {
        User user = User.builder()
                .email(info.email())
                .role(UserRole.GUEST)
                .name(info.name())
                .picture(info.profile())
                .provider(provider)
                .providerId(providerId)
                .build();

        return userRepository.save(user);
    }


}
