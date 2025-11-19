package com.gpt.geumpumtabackend.global.config.security;



import com.gpt.geumpumtabackend.global.jwt.JwtAuthenticationFilter;
import com.gpt.geumpumtabackend.global.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.gpt.geumpumtabackend.global.oauth.resolver.CustomAuthorizationRequestResolver;
import com.gpt.geumpumtabackend.global.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final AuthenticationManager authenticationManager;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient;

    @Bean
    public SecurityFilterChain filterChainPermitAll(HttpSecurity http) throws Exception {
        return defaultSecurity(http)
                // 일시적으로 모든 URL에 대해 권한 확인을 시행하지 않음
                .authorizeHttpRequests(req ->
                        req.anyRequest().permitAll())
                .build();
    }

    public HttpSecurity defaultSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(CorsConfig.corsConfigurationSource()))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth ->
                                auth.authorizationRequestResolver(customAuthorizationRequestResolver))
                        .tokenEndpoint(token ->
                                token.accessTokenResponseClient(authorizationCodeTokenResponseClient))
                        .userInfoEndpoint(ui ->
                                ui.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                .addFilterAfter(new JwtAuthenticationFilter(authenticationManager),
                        UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl
                .withDefaultRolePrefix()  // 기본 접두어 "ROLE_" 사용
                .role("ADMIN")
                .implies("USER")
                .build();
    }

    @Bean
    public MethodSecurityExpressionHandler expressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

}