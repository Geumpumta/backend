package com.gpt.geumpumtabackend.integration.rank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.study.domain.StudySession;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("DepartmentRank Controller 통합 테스트")
@AutoConfigureMockMvc
class DepartmentRankControllerIntegrationTest extends BaseIntegrationTest {

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

    @Autowired
    private DepartmentRankingRepository departmentRankingRepository;

    private User softwareUser1;
    private User softwareUser2;
    private User computerUser;
    private User electronicUser;
    private String softwareUserToken;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 - 올바른 빌더 사용
        softwareUser1 = createUser("소프트웨어1", "sw1@kumoh.ac.kr", Department.SOFTWARE);
        softwareUser2 = createUser("소프트웨어2", "sw2@kumoh.ac.kr", Department.SOFTWARE);
        computerUser = createUser("컴퓨터공학", "ce@kumoh.ac.kr", Department.COMPUTER_ENGINEERING);
        electronicUser = createUser("전자공학", "ee@kumoh.ac.kr", Department.ELECTRONIC_SYSTEMS);

        // 소프트웨어 유저 토큰 생성
        JwtUserClaim claim = new JwtUserClaim(softwareUser1.getId(), UserRole.USER, false);
        Token token = jwtHandler.createTokens(claim);
        softwareUserToken = token.getAccessToken();
    }

    private User createUser(String name, String email, Department department) {
        User user = User.builder()
                .name(name)
                .email(email)
                .department(department)
                .role(UserRole.USER)
                .picture("profile.jpg")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("provider-" + email)
                .build();
        return userRepository.save(user);
    }

    private void createStudySession(User user, LocalDateTime startTime, long durationHours) {
        LocalDateTime endTime = startTime.plusHours(durationHours);

        StudySession session = new StudySession();
        session.startStudySession(startTime, user);
        session.endStudySession(endTime);
        studySessionRepository.save(session);
    }

    @Nested
    @DisplayName("일간 학과 랭킹 조회 API")
    class GetDailyDepartmentRanking {

        @Test
        @DisplayName("현재_진행중인_일간_랭킹을_조회한다")
        void 현재_진행중인_일간_랭킹을_조회한다() throws Exception {
            // Given - 오늘의 학습 기록
            LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0);
            createStudySession(softwareUser1, today, 3); // 소프트웨어: 3시간
            createStudySession(softwareUser2, today, 2); // 소프트웨어: 2시간 (총 5시간)
            createStudySession(computerUser, today, 4);   // 컴퓨터공학: 4시간
            createStudySession(electronicUser, today, 1); // 전자공학: 1시간

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/daily")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isArray())
                    .andExpect(jsonPath("$.data.topRanks", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.data.topRanks[0].departmentName").exists())
                    .andExpect(jsonPath("$.data.topRanks[0].totalMillis").value(greaterThan(0)))
                    .andExpect(jsonPath("$.data.myDepartmentRanking").exists())
                    .andExpect(jsonPath("$.data.myDepartmentRanking.departmentName").value("소프트웨어전공"));
        }

        @Test
        @DisplayName("특정_날짜의_확정된_일간_랭킹을_조회한다")
        void 특정_날짜의_확정된_일간_랭킹을_조회한다() throws Exception {
            // Given - 어제의 학습 기록
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0);
            createStudySession(softwareUser1, yesterday, 5);
            createStudySession(computerUser, yesterday, 3);

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/daily")
                            .param("date", yesterday.toString())
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isArray())
                    .andExpect(jsonPath("$.data.myDepartmentRanking").exists());
        }

        @Test
        @DisplayName("인증_없이_요청하면_403_에러가_발생한다")
        void 인증_없이_요청하면_403_에러가_발생한다() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/daily"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("아무도_학습하지_않은_날에는_빈_랭킹을_반환한다")
        void 아무도_학습하지_않은_날에는_빈_랭킹을_반환한다() throws Exception {
            // Given - 학습 기록 없음

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/daily")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isEmpty())
                    .andExpect(jsonPath("$.data.myDepartmentRanking").exists())
                    .andExpect(jsonPath("$.data.myDepartmentRanking.rank").value(1))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.totalMillis").value(0));
        }
    }

    @Nested
    @DisplayName("주간 학과 랭킹 조회 API")
    class GetWeeklyDepartmentRanking {

        @Test
        @DisplayName("현재_진행중인_주간_랭킹을_조회한다")
        void 현재_진행중인_주간_랭킹을_조회한다() throws Exception {
            // Given - 이번 주의 학습 기록
            LocalDateTime thisWeek = LocalDateTime.now().withHour(10).withMinute(0);
            createStudySession(softwareUser1, thisWeek, 10);
            createStudySession(softwareUser2, thisWeek.minusDays(1), 8);
            createStudySession(computerUser, thisWeek, 7);

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/weekly")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isArray())
                    .andExpect(jsonPath("$.data.myDepartmentRanking").exists());
        }

        @Test
        @DisplayName("특정_날짜가_포함된_주의_확정된_주간_랭킹을_조회한다")
        void 특정_날짜가_포함된_주의_확정된_주간_랭킹을_조회한다() throws Exception {
            // Given - 지난 주의 학습 기록
            LocalDateTime lastWeek = LocalDateTime.now().minusWeeks(1).withHour(10).withMinute(0);
            createStudySession(softwareUser1, lastWeek, 20);
            createStudySession(computerUser, lastWeek, 15);

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/weekly")
                            .param("date", lastWeek.toString())
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isArray());
        }
    }

    @Nested
    @DisplayName("월간 학과 랭킹 조회 API")
    class GetMonthlyDepartmentRanking {

        @Test
        @DisplayName("현재_진행중인_월간_랭킹을_조회한다")
        void 현재_진행중인_월간_랭킹을_조회한다() throws Exception {
            // Given - 이번 달의 학습 기록
            LocalDateTime thisMonth = LocalDateTime.now().withHour(10).withMinute(0);
            createStudySession(softwareUser1, thisMonth, 50);
            createStudySession(softwareUser2, thisMonth.minusDays(5), 40);
            createStudySession(computerUser, thisMonth, 30);

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/monthly")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isArray())
                    .andExpect(jsonPath("$.data.myDepartmentRanking").exists());
        }

        @Test
        @DisplayName("특정_날짜가_포함된_월의_확정된_월간_랭킹을_조회한다")
        void 특정_날짜가_포함된_월의_확정된_월간_랭킹을_조회한다() throws Exception {
            // Given - 지난 달의 학습 기록
            LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1).withHour(10).withMinute(0);
            createStudySession(softwareUser1, lastMonth, 100);
            createStudySession(computerUser, lastMonth, 80);

            // When & Then
            mockMvc.perform(get("/api/v1/rank/department/monthly")
                            .param("date", lastMonth.toString())
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.topRanks").isArray());
        }
    }

    @Nested
    @DisplayName("Controller-Service-Repository 전체 흐름 테스트")
    class FullFlowTest {

        @Test
        @DisplayName("학습_기록부터_랭킹_조회까지_전체_흐름이_정상_동작한다")
        void 학습_기록부터_랭킹_조회까지_전체_흐름이_정상_동작한다() throws Exception {
            // 1. 여러 학과 학생들이 학습
            LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0);
            createStudySession(softwareUser1, today, 5);
            createStudySession(softwareUser2, today, 3);  // 소프트웨어: 총 8시간
            createStudySession(computerUser, today, 6);    // 컴퓨터공학: 6시간
            createStudySession(electronicUser, today, 2);  // 전자공학: 2시간

            // 2. 일간 랭킹 조회 - 소프트웨어가 1등이어야 함
            mockMvc.perform(get("/api/v1/rank/department/daily")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.topRanks[0].departmentName").value("소프트웨어전공"))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.rank").value(1));

            // 3. 전자공학 학생 토큰으로 조회 - 같은 랭킹이지만 내 학과는 다름
            JwtUserClaim electronicClaim = new JwtUserClaim(electronicUser.getId(), UserRole.USER, false);
            Token electronicToken = jwtHandler.createTokens(electronicClaim);

            mockMvc.perform(get("/api/v1/rank/department/daily")
                            .header("Authorization", "Bearer " + electronicToken.getAccessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.topRanks[0].departmentName").value("소프트웨어전공"))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.departmentName").value("전자시스템전공"))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.rank").value(greaterThan(1)));

            // 4. 주간 랭킹도 정상 조회
            mockMvc.perform(get("/api/v1/rank/department/weekly")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.topRanks").isArray());

            // 5. 월간 랭킹도 정상 조회
            mockMvc.perform(get("/api/v1/rank/department/monthly")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.topRanks").isArray());
        }

        @Test
        @DisplayName("다른_사용자가_조회해도_같은_랭킹을_보지만_내_학과_정보만_다르다")
        void 다른_사용자가_조회해도_같은_랭킹을_보지만_내_학과_정보만_다르다() throws Exception {
            // Given
            LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0);
            createStudySession(softwareUser1, today, 10);
            createStudySession(computerUser, today, 8);

            // 소프트웨어 유저로 조회
            String swResponse = mockMvc.perform(get("/api/v1/rank/department/daily")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // 컴퓨터공학 유저로 조회
            JwtUserClaim computerClaim = new JwtUserClaim(computerUser.getId(), UserRole.USER, false);
            Token computerToken = jwtHandler.createTokens(computerClaim);

            String ceResponse = mockMvc.perform(get("/api/v1/rank/department/daily")
                            .header("Authorization", "Bearer " + computerToken.getAccessToken()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // 전체 랭킹은 동일하지만, myDepartmentRanking은 달라야 함
            var swData = objectMapper.readTree(swResponse).get("data");
            var ceData = objectMapper.readTree(ceResponse).get("data");

            // topRanks는 동일
            assertThat(swData.get("topRanks").size()).isEqualTo(ceData.get("topRanks").size());

            // myDepartmentRanking은 다름
            assertThat(swData.get("myDepartmentRanking").get("departmentName").asText())
                    .isNotEqualTo(ceData.get("myDepartmentRanking").get("departmentName").asText());
        }
    }
}
