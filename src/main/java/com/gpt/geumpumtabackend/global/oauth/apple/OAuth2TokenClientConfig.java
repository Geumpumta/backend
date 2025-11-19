package com.gpt.geumpumtabackend.global.oauth.apple;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

@Configuration
@RequiredArgsConstructor
public class OAuth2TokenClientConfig {

    private final AppleClientSecretGenerator appleClientSecretGenerator;

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
    authorizationCodeTokenResponseClient() {

        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();

        DefaultOAuth2TokenRequestParametersConverter<OAuth2AuthorizationCodeGrantRequest> defaultConverter =
                new DefaultOAuth2TokenRequestParametersConverter<>();

        client.setParametersConverter(request -> {
            MultiValueMap<String, String> params = defaultConverter.convert(request);

            String registrationId = request.getClientRegistration().getRegistrationId();

            // 애플일 때만 client_secret을 JWT로 교체
            if ("apple".equals(registrationId)) {
                String clientSecretJwt = appleClientSecretGenerator.generate();
                params.set(OAuth2ParameterNames.CLIENT_SECRET, clientSecretJwt);
            }

            // 구글/카카오는 기존 clientSecret 그대로 사용
            return params;
        });

        return client;
    }
}
