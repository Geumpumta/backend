package com.gpt.geumpumtabackend.integration.study.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.domain.StudyStatus;
import com.gpt.geumpumtabackend.study.dto.request.StudyEndRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("StudySession Controller 통합 테스트")
@AutoConfigureMockMvc
class  StudySessionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtHandler jwtHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 - 올바른 빌더 사용
        testUser = User.builder()
                .name("테스트유저")
                .email("test@kumoh.ac.kr")
                .department(Department.SOFTWARE)
                .role(UserRole.USER)
                .picture("test.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("test-provider-id")
                .build();
        testUser = userRepository.save(testUser);

        // JWT 토큰 생성
        JwtUserClaim claim = new JwtUserClaim(testUser.getId(), UserRole.USER, false);
        Token token = jwtHandler.createTokens(claim);
        accessToken = token.getAccessToken();
    }

    @Nested
    @DisplayName("공부 시작 API")
    class StartStudySession {

        @Test
        @DisplayName("정상적으로_공부를_시작하고_세션ID를_반환한다")
        void 정상적으로_공부를_시작하고_세션ID를_반환한다() throws Exception {
            // Given
            StudyStartRequest request = new StudyStartRequest("172.30.64.1", "172.30.64.100");

            // When & Then
            mockMvc.perform(post("/api/v1/study/start")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.studySessionId").exists())
                    .andExpect(jsonPath("$.data.studySessionId").isNumber());

            // DB 검증
            StudySession savedSession = studySessionRepository.findAll().get(0);
            assertThat(savedSession.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(savedSession.getStatus()).isEqualTo(StudyStatus.STARTED);
            assertThat(savedSession.getStartTime()).isNotNull();
        }

        @Test
        @DisplayName("인증_없이_요청하면_403_에러가_발생한다")
        void 인증_없이_요청하면_403_에러가_발생한다() throws Exception {
            // Given
            StudyStartRequest request = new StudyStartRequest("172.30.64.1", "172.30.64.100");

            // When & Then
            mockMvc.perform(post("/api/v1/study/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("잘못된_토큰으로_요청하면_401_에러가_발생한다")
        void 잘못된_토큰으로_요청하면_401_에러가_발생한다() throws Exception {
            // Given
            StudyStartRequest request = new StudyStartRequest("172.30.64.1", "172.30.64.100");

            // When & Then
            mockMvc.perform(post("/api/v1/study/start")
                            .header("Authorization", "Bearer invalid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("공부 종료 API")
    class EndStudySession {

        @Test
        @DisplayName("정상적으로_공부를_종료하고_시간을_계산한다")
        void 정상적으로_공부를_종료하고_시간을_계산한다() throws Exception {
            // Given - 먼저 공부 시작 (올바른 도메인 생성 방법)
            LocalDateTime startTime = LocalDateTime.now().minusHours(2);
            StudySession session = new StudySession();
            session.startStudySession(startTime, testUser);
            session = studySessionRepository.save(session);

            StudyEndRequest request = new StudyEndRequest(session.getId());

            // When & Then
            mockMvc.perform(post("/api/v1/study/end")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"));

            // DB 검증
            StudySession endedSession = studySessionRepository.findById(session.getId()).orElseThrow();
            assertThat(endedSession.getStatus()).isEqualTo(StudyStatus.FINISHED);
            assertThat(endedSession.getEndTime()).isNotNull();
            assertThat(endedSession.getTotalMillis()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("오늘의 공부 세션 조회 API")
    class GetTodayStudySession {

        @Test
        @DisplayName("오늘의_공부_기록을_조회한다")
        void 오늘의_공부_기록을_조회한다() throws Exception {
            // Given - 오늘 공부 기록 생성 (현재 시각 이전으로 설정)
            LocalDateTime endTime = LocalDateTime.now().minusHours(1);  // 1시간 전에 종료
            LocalDateTime startTime = endTime.minusHours(2);  // 3시간 전에 시작

            StudySession session = new StudySession();
            session.startStudySession(startTime, testUser);
            session.endStudySession(endTime);
            studySessionRepository.save(session);

            // When & Then
            mockMvc.perform(get("/api/v1/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.totalStudySession").value(greaterThan(0)));
        }

        @Test
        @DisplayName("공부_기록이_없으면_빈_응답을_반환한다")
        void 공부_기록이_없으면_빈_응답을_반환한다() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("다른_사용자의_공부_기록은_조회되지_않는다")
        void 다른_사용자의_공부_기록은_조회되지_않는다() throws Exception {
            // Given - 다른 사용자의 공부 기록
            User otherUser = User.builder()
                    .name("다른유저")
                    .email("other@kumoh.ac.kr")
                    .department(Department.COMPUTER_ENGINEERING)
                    .role(UserRole.USER)
                    .picture("other.jpg")
                    .provider(OAuth2Provider.GOOGLE)
                    .providerId("other-provider-id")
                    .build();
            otherUser = userRepository.save(otherUser);

            LocalDateTime endTime = LocalDateTime.now().minusHours(1);
            LocalDateTime startTime = endTime.minusHours(3);

            StudySession otherSession = new StudySession();
            otherSession.startStudySession(startTime, otherUser);
            otherSession.endStudySession(endTime);
            studySessionRepository.save(otherSession);

            // When & Then - 내 토큰으로 조회하면 다른 사람 기록은 안 보임
            mockMvc.perform(get("/api/v1/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.totalStudySession").value(0));
        }
    }

    @Nested
    @DisplayName("Controller-Service-Repository 전체 흐름 테스트")
    class FullFlowTest {

        @Test
        @DisplayName("공부_시작부터_종료까지_전체_흐름이_정상_동작한다")
        void 공부_시작부터_종료까지_전체_흐름이_정상_동작한다() throws Exception {
            // 1. 공부 시작
            StudyStartRequest startRequest = new StudyStartRequest("172.30.64.1", "172.30.64.100");
            String startResponse = mockMvc.perform(post("/api/v1/study/start")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long sessionId = objectMapper.readTree(startResponse)
                    .get("data")
                    .get("studySessionId")
                    .asLong();

            // 2. 오늘의 공부 세션 조회 (진행중)
            mockMvc.perform(get("/api/v1/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").exists());

            // 3. 공부 종료
            StudyEndRequest endRequest = new StudyEndRequest(sessionId);
            mockMvc.perform(post("/api/v1/study/end")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(endRequest)))
                    .andExpect(status().isOk());

            // 4. 다시 조회 (종료됨)
            mockMvc.perform(get("/api/v1/study")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalStudySession").value(greaterThan(0)));

            // 5. DB 최종 검증
            StudySession finalSession = studySessionRepository.findById(sessionId).orElseThrow();
            assertThat(finalSession.getStatus()).isEqualTo(StudyStatus.FINISHED);
            assertThat(finalSession.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(finalSession.getStartTime()).isNotNull();
            assertThat(finalSession.getEndTime()).isNotNull();
            assertThat(finalSession.getTotalMillis()).isGreaterThan(0);
        }
    }
}
