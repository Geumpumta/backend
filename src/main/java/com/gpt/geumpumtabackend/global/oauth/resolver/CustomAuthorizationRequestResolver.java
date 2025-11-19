package com.gpt.geumpumtabackend.global.oauth.resolver;



import com.gpt.geumpumtabackend.global.oauth.util.RedirectUrlValidator;
import com.gpt.geumpumtabackend.global.oauth.util.StateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest original = delegate.resolve(request);
        return customizeState(request, original);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest original = delegate.resolve(request, clientRegistrationId);
        return customizeState(request, original);
    }

    private OAuth2AuthorizationRequest customizeState(HttpServletRequest request,
                                                      OAuth2AuthorizationRequest original) {
        if (original == null) return null;

        // 프론트에서 ?redirect_uri=... 로 넘긴 값
        String rawRedirect = request.getParameter("redirect_uri");
        if (rawRedirect == null || rawRedirect.isBlank()) {
            return original; // redirect_uri 없이도 로그인 가능하도록
        }

        // 화이트리스트 검증
        RedirectUrlValidator.validate(rawRedirect);

        // 인코딩
        String encodedState = StateUtil.encode(rawRedirect);

        String registrationId = original.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);

        Map<String, Object> additional =
                new LinkedHashMap<>(original.getAdditionalParameters());

        // 3) 애플이면 response_mode=form_post 추가
        if ("apple".equalsIgnoreCase(registrationId)) {
            additional.put("response_mode", "form_post");
        }

        return OAuth2AuthorizationRequest.from(original)
                .state(encodedState)
                .additionalParameters(additional)  // 🔹 여기에만 response_mode 들어감
                .build();
    }
}
