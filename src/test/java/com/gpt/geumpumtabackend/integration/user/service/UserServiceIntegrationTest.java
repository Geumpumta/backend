package com.gpt.geumpumtabackend.integration.user.service;

import com.gpt.geumpumtabackend.badge.domain.Badge;
import com.gpt.geumpumtabackend.badge.domain.BadgeType;
import com.gpt.geumpumtabackend.badge.repository.BadgeRepository;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserService 통합 테스트")
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Test
    @DisplayName("회원가입 완료 직후 랜덤 닉네임이 DB에 저장된다")
    void completeRegistration_persistsRandomNickname() {
        User guest = userRepository.saveAndFlush(User.builder()
                .email("oauth@test.com")
                .role(UserRole.GUEST)
                .name("테스트사용자")
                .picture("profile.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("provider-id")
                .build());
        badgeRepository.saveAndFlush(Badge.builder()
                .code("WELCOME_001")
                .name("웰컴 배지")
                .description("회원가입 기념 배지")
                .iconUrl("https://example.com/welcome.png")
                .badgeType(BadgeType.WELCOME)
                .build());

        CompleteRegistrationRequest request = new CompleteRegistrationRequest(
                "test@kumoh.ac.kr",
                "20240001",
                Department.SOFTWARE.getEnglishName()
        );

        userService.completeRegistration(request, guest.getId());

        User savedUser = userRepository.findById(guest.getId()).orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getSchoolEmail()).isEqualTo(request.email());
        assertThat(savedUser.getStudentId()).isEqualTo(request.studentId());
        assertThat(savedUser.getDepartment()).isEqualTo(Department.SOFTWARE);
        assertThat(savedUser.getNickname()).isNotBlank();
    }
}
