package com.gpt.geumpumtabackend.global.oauth.handler;

import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.service.OAuthLoginPolicyService;
import com.gpt.geumpumtabackend.global.oauth.service.OAuth2UserPrincipal;
import com.gpt.geumpumtabackend.global.oauth.util.RedirectUrlValidator;
import com.gpt.geumpumtabackend.global.oauth.util.StateUtil;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.token.domain.Token;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthLoginPolicyService oAuthLoginPolicyService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {


        String encodedState = request.getParameter("state");
        String redirectUri = StateUtil.decode(encodedState);

        // 2) 화이트리스트 검증
        RedirectUrlValidator.validate(redirectUri);

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getUser().getId();
        UserRole role = principal.getUser().getRole();
        Boolean isWithdrawn = principal.getUser().getDeletedAt() != null;

        JwtUserClaim jwtUserClaim = new JwtUserClaim(userId, role, isWithdrawn);
        Optional<Token> token = oAuthLoginPolicyService.issueTokenIfNoActiveSession(jwtUserClaim);
        if (token.isEmpty()) {
            String blockedUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "already_logged_in")
                    .build().toUriString();
            response.sendRedirect(blockedUrl);
            return;
        }
        Token issuedToken = token.get();

        // 토큰 붙여서 리다이렉트
        String redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", issuedToken.getAccessToken())
                .queryParam("refreshToken", issuedToken.getRefreshToken())
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }
}
