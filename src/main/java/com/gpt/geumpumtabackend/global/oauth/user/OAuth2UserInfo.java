package com.gpt.geumpumtabackend.global.oauth.user;



import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
@Slf4j
@Builder
public record OAuth2UserInfo(
        String name,
        String email,
        String profile
) {

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) { // registration id별로 userInfo 생성
            case "google" -> ofGoogle(attributes);
            case "kakao" -> ofKakao(attributes);
            default -> throw new BusinessException(ExceptionType.ILLEGAL_REGISTRATION_ID);
        };
    }

    private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
        log.info("Google User Info: {}", attributes.get("name"));
        log.info("Google User Info: {}", attributes.get("email"));
        log.info("Google User Info: {}", attributes.get("picture"));

        return OAuth2UserInfo.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profile((String) attributes.get("picture"))
                .build();
    }

    // TODO : 구글 도입 후에 카카오 도입
    private static OAuth2UserInfo ofKakao(Map<String, Object> attributes) {
        log.info("Kakao User Info: {}", attributes.get("kakao_account"));
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");


        log.info("Kakao User Info: {}", account.get("nickname"));
        log.info("Kakao User Info: {}", account.get("email"));
        log.info("Kakao User Info: {}", account.get("profile_image"));

        Map<String, Object> properties = (Map<String, Object>) account.get("profile");

        return OAuth2UserInfo.builder()
                .name((String) properties.get("nickname"))
                .email((String) account.get("email"))
                .profile((String) properties.get("profile_image_url"))
                .build();
    }


//    public User toEntity() {
//        return User.builder()
//                .email(email)
//                .role(USER)
//                .provider(provider)
//                .build()
//    }
}