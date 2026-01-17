package com.gpt.geumpumtabackend.unit.user.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.token.repository.RefreshTokenRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import com.gpt.geumpumtabackend.user.dto.request.NicknameVerifyRequest;
import com.gpt.geumpumtabackend.user.dto.request.ProfileUpdateRequest;
import com.gpt.geumpumtabackend.user.dto.response.UserProfileResponse;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtHandler jwtHandler;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("관리자 여부 확인")
    class IsAdmin {

        @Test
        @DisplayName("관리자 권한 사용자면 true를 반환한다")
        void 관리자면_true_반환() {
            // Given
            Long userId = 1L;
            User adminUser = createTestUser(userId, UserRole.ADMIN);
            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));

            // When
            boolean result = userService.isAdmin(userId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("관리자 권한이 아니면 false를 반환한다")
        void 관리자가아니면_false_반환() {
            // Given
            Long userId = 1L;
            User normalUser = createTestUser(userId, UserRole.USER);
            given(userRepository.findById(userId)).willReturn(Optional.of(normalUser));

            // When
            boolean result = userService.isAdmin(userId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("랜덤 닉네임 생성")
    class GenerateRandomNickname {

        @Test
        @DisplayName("중복 닉네임이 있으면 재생성 후 닉네임이 설정된다")
        void 중복닉네임_재생성후_닉네임설정() {
            // Given
            User user = createTestUser(1L, UserRole.USER);
            given(userRepository.existsByNickname(any())).willReturn(true, false);

            // When
            userService.generateRandomNickname(user);

            // Then
            assertThat(user.getNickname()).isNotBlank();
            verify(userRepository, times(2)).existsByNickname(any());
        }
    }

    @Nested
    @DisplayName("회원가입 완료")
    class CompleteRegistration {

        @Test
        @DisplayName("회원가입 완료 시 사용자 정보가 갱신되고 토큰이 반환된다")
        void 회원가입완료_정상처리() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.GUEST);
            CompleteRegistrationRequest request = new CompleteRegistrationRequest(
                    "test@kumoh.ac.kr",
                    "20240001",
                    "Software Engineering"
            );

            Token token = Token.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsBySchoolEmail(request.email())).willReturn(false);
            given(userRepository.existsByStudentId(request.studentId())).willReturn(false);
            given(userRepository.existsByNickname(any())).willReturn(false);
            given(jwtHandler.createTokens(any(JwtUserClaim.class))).willReturn(token);

            // When
            TokenResponse response = userService.completeRegistration(request, userId);

            // Then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(user.getSchoolEmail()).isEqualTo(request.email());
            assertThat(user.getStudentId()).isEqualTo(request.studentId());
            assertThat(user.getDepartment()).isEqualTo(Department.SOFTWARE);
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getNickname()).isNotBlank();
            verify(jwtHandler).createTokens(argThat(claim ->
                    claim.userId().equals(userId) &&
                            claim.role().equals(UserRole.USER) &&
                            !claim.withdrawn()
            ));
        }

        @Test
        @DisplayName("이메일이 중복이면 DUPLICATED_SCHOOL_EMAIL 예외가 발생한다")
        void 이메일중복_예외발생() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.GUEST);
            CompleteRegistrationRequest request = new CompleteRegistrationRequest(
                    "test@kumoh.ac.kr",
                    "20240001",
                    "Software Engineering"
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsBySchoolEmail(request.email())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.completeRegistration(request, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.DUPLICATED_SCHOOL_EMAIL);
        }

        @Test
        @DisplayName("학번이 중복이면 DUPLICATED_STUDENT_ID 예외가 발생한다")
        void 학번중복_예외발생() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.GUEST);
            CompleteRegistrationRequest request = new CompleteRegistrationRequest(
                    "test@kumoh.ac.kr",
                    "20240001",
                    "Software Engineering"
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsBySchoolEmail(request.email())).willReturn(false);
            given(userRepository.existsByStudentId(request.studentId())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.completeRegistration(request, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.DUPLICATED_STUDENT_ID);
        }
    }

    @Nested
    @DisplayName("사용자 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("사용자 프로필 조회 시 사용자 정보가 반환된다")
        void 프로필조회_정상반환() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.USER);
            setField(user, "nickname", "tester");
            setField(user, "schoolEmail", "school@kumoh.ac.kr");
            setField(user, "studentId", "20240001");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            UserProfileResponse response = userService.getUserProfile(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.email()).isEqualTo(user.getEmail());
            assertThat(response.schoolEmail()).isEqualTo("school@kumoh.ac.kr");
            assertThat(response.userRole()).isEqualTo("USER");
            assertThat(response.name()).isEqualTo(user.getName());
            assertThat(response.nickName()).isEqualTo("tester");
            assertThat(response.profilePictureUrl()).isEqualTo(user.getPicture());
            assertThat(response.OAuthProvider()).isEqualTo("GOOGLE");
            assertThat(response.studentId()).isEqualTo("20240001");
            assertThat(response.department()).isEqualTo("소프트웨어전공");
        }
    }

    @Nested
    @DisplayName("닉네임 사용 가능 여부")
    class IsNicknameAvailable {

        @Test
        @DisplayName("닉네임이 중복이면 false를 반환한다")
        void 닉네임중복_false_반환() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.USER);
            NicknameVerifyRequest request = new NicknameVerifyRequest("tester");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname(request.nickname())).willReturn(true);

            // When
            boolean result = userService.isNicknameAvailable(request, userId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("프로필 업데이트")
    class UpdateUserProfile {

        @Test
        @DisplayName("프로필 업데이트 시 사용자 정보가 변경된다")
        void 프로필업데이트_정상처리() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.USER);
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "https://image.test/profile.png",
                    "public-id",
                    "newname"
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            userService.updateUserProfile(request, userId);

            // Then
            assertThat(user.getPicture()).isEqualTo("https://image.test/profile.png");
            assertThat(user.getPublicId()).isEqualTo("public-id");
            assertThat(user.getNickname()).isEqualTo("newname");
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃 시 리프레시 토큰이 삭제된다")
        void 로그아웃_정상처리() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.USER);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            userService.logout(userId);

            // Then
            verify(refreshTokenRepository).deleteByUserId(userId);
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void 사용자없음_예외발생() {
            // Given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.logout(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class WithdrawUser {

        @Test
        @DisplayName("회원 탈퇴 시 사용자와 리프레시 토큰이 삭제된다")
        void 회원탈퇴_정상처리() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.USER);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            userService.withdrawUser(userId);

            // Then
            verify(refreshTokenRepository).deleteByUserId(userId);
            verify(userRepository).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("회원 복구")
    class RestoreUser {

        @Test
        @DisplayName("회원 복구 시 삭제 prefix가 제거되고 토큰이 발급된다")
        void 회원복구_정상처리() {
            // Given
            Long userId = 1L;
            User user = createTestUser(userId, UserRole.USER);
            setField(user, "nickname", "deleted_nick");
            setField(user, "email", "deleted_user@kumoh.ac.kr");
            setField(user, "schoolEmail", "deleted_school@kumoh.ac.kr");
            setField(user, "studentId", "deleted_20240001");
            setField(user, "deletedAt", LocalDateTime.now());

            Token token = Token.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(jwtHandler.createTokens(any(JwtUserClaim.class))).willReturn(token);

            // When
            TokenResponse response = userService.restoreUser(userId);

            // Then
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(user.getNickname()).isEqualTo("nick");
            assertThat(user.getEmail()).isEqualTo("user@kumoh.ac.kr");
            assertThat(user.getSchoolEmail()).isEqualTo("school@kumoh.ac.kr");
            assertThat(user.getStudentId()).isEqualTo("20240001");
            assertThat(user.getDeletedAt()).isNull();
            verify(jwtHandler).createTokens(argThat(claim ->
                    claim.userId().equals(userId) && !claim.withdrawn()
            ));
        }
    }

    @Nested
    @DisplayName("삭제 prefix 제거")
    class RemoveDeletedPrefix {

        @Test
        @DisplayName("prefix가 있으면 삭제하고 없으면 그대로 반환한다")
        void prefix_처리_정상() {
            // Given
            String prefixed = "deleted_value";
            String plain = "value";

            // When
            String removed = userService.removeDeletedPrefix(prefixed);
            String unchanged = userService.removeDeletedPrefix(plain);

            // Then
            assertThat(removed).isEqualTo("value");
            assertThat(unchanged).isEqualTo("value");
        }

        @Test
        @DisplayName("null 입력이면 null을 반환한다")
        void null_입력_반환() {
            // When
            String result = userService.removeDeletedPrefix(null);

            // Then
            assertThat(result).isNull();
        }
    }

    private User createTestUser(Long id, UserRole role) {
        User user = User.builder()
                .name("테스트사용자")
                .email("user@kumoh.ac.kr")
                .department(Department.SOFTWARE)
                .picture("profile.jpg")
                .role(role)
                .provider(OAuth2Provider.GOOGLE)
                .providerId("provider-id")
                .build();

        setField(user, "id", id);
        return user;
    }

    private void setField(User user, String fieldName, Object value) {
        try {
            Class<?> current = user.getClass();
            while (current != null) {
                try {
                    java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(user, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException("Field not found: " + fieldName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field for test: " + fieldName, e);
        }
    }
}
