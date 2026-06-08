package com.gpt.geumpumtabackend.integration.statistics;

import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.service.UserSessionService;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Statistics Controller 통합 테스트")
@AutoConfigureMockMvc
class StatisticsControllerIntegrationTest extends BaseIntegrationTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2024, 1, 10);
    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtHandler jwtHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private UserSessionService userSessionService;

    private User testUser;
    private User otherUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = createUser("테스트유저", "stats@kumoh.ac.kr", Department.SOFTWARE);
        otherUser = createUser("다른유저", "other-stats@kumoh.ac.kr", Department.COMPUTER_ENGINEERING);

        accessToken = createAccessToken(testUser);
    }

    private String createAccessToken(User user) {
        UserSession userSession = userSessionService.createNewSession(user.getId(), 3600);
        JwtUserClaim claim = JwtUserClaim.create(user, userSession.getSessionId());
        Token token = jwtHandler.createTokens(claim, userSession.getRefreshToken());
        return token.getAccessToken();
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

    private StudySession createStudySession(User user, LocalDateTime startTime, LocalDateTime endTime) {
        StudySession session = new StudySession();
        session.startStudySession(startTime, user);
        session.endStudySession(endTime);
        return studySessionRepository.save(session);
    }

    @Test
    @DisplayName("일간 통계를 2시간 슬롯과 최대 집중 시간으로 조회한다")
    void 일간_통계를_조회한다() throws Exception {
        LocalDateTime startTime = BASE_DATE.atTime(1, 0);
        LocalDateTime endTime = BASE_DATE.atTime(3, 0);
        createStudySession(testUser, startTime, endTime);

        createStudySession(otherUser, BASE_DATE.atTime(2, 0), BASE_DATE.atTime(4, 0));

        mockMvc.perform(get("/api/v1/statistics/day")
                        .param("date", BASE_DATE.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data.statisticsList", hasSize(12)))
                .andExpect(jsonPath("$.data.statisticsList[0].slotStart").value("00:00"))
                .andExpect(jsonPath("$.data.statisticsList[0].slotEnd").value("02:00"))
                .andExpect(jsonPath("$.data.statisticsList[0].millisecondsStudied").value((int) ONE_HOUR_MILLIS))
                .andExpect(jsonPath("$.data.statisticsList[1].slotStart").value("02:00"))
                .andExpect(jsonPath("$.data.statisticsList[1].slotEnd").value("04:00"))
                .andExpect(jsonPath("$.data.statisticsList[1].millisecondsStudied").value((int) ONE_HOUR_MILLIS))
                .andExpect(jsonPath("$.data.dayMaxFocusAndFullTimeStatistics.totalStudyMillis").value((int) (ONE_HOUR_MILLIS * 2)))
                .andExpect(jsonPath("$.data.dayMaxFocusAndFullTimeStatistics.maxFocusMillis").value((int) (ONE_HOUR_MILLIS * 2)));
    }

    @Test
    @DisplayName("주간 통계의 총합/연속일/평균을 계산한다")
    void 주간_통계를_조회한다() throws Exception {
        LocalDate weekDate = BASE_DATE;
        LocalDate monday = weekDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        createStudySession(testUser, monday.atTime(10, 0), monday.atTime(11, 0));
        createStudySession(testUser, monday.plusDays(1).atTime(12, 0), monday.plusDays(1).atTime(13, 0));

        mockMvc.perform(get("/api/v1/statistics/week")
                        .param("date", weekDate.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data.weeklyStatistics.totalWeekMillis").value((int) (ONE_HOUR_MILLIS * 2)))
                .andExpect(jsonPath("$.data.weeklyStatistics.maxConsecutiveStudyDays").value(2))
                .andExpect(jsonPath("$.data.weeklyStatistics.averageDailyMillis").value(1_028_571));
    }

    @Test
    @DisplayName("월간 통계의 총합/연속일/평균/공부일수를 계산한다")
    void 월간_통계를_조회한다() throws Exception {
        createStudySession(testUser, BASE_DATE.withDayOfMonth(2).atTime(9, 0), BASE_DATE.withDayOfMonth(2).atTime(10, 0));
        createStudySession(testUser, BASE_DATE.withDayOfMonth(3).atTime(14, 0), BASE_DATE.withDayOfMonth(3).atTime(16, 0));
        createStudySession(testUser, BASE_DATE.withDayOfMonth(5).atTime(20, 0), BASE_DATE.withDayOfMonth(5).atTime(21, 0));

        mockMvc.perform(get("/api/v1/statistics/month")
                        .param("date", BASE_DATE.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data.monthlyStatistics.totalMonthMillis").value((int) 14_400_000L))
                .andExpect(jsonPath("$.data.monthlyStatistics.averageDailyMillis").value(464_516))
                .andExpect(jsonPath("$.data.monthlyStatistics.maxConsecutiveStudyDays").value(2))
                .andExpect(jsonPath("$.data.monthlyStatistics.studiedDays").value(3));
    }

    @Test
    @DisplayName("잔디 통계는 4개월 범위의 날짜별 레벨을 반환한다")
    void 잔디_통계를_조회한다() throws Exception {
        createStudySession(testUser, BASE_DATE.withDayOfMonth(2).atTime(9, 0), BASE_DATE.withDayOfMonth(2).atTime(10, 0));

        mockMvc.perform(get("/api/v1/statistics/grass")
                        .param("date", BASE_DATE.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"))
                .andExpect(jsonPath("$.data.grassStatistics", hasSize(123)))
                .andExpect(jsonPath("$.data.grassStatistics[?(@.date=='2024-01-02')].level",
                        hasItem(greaterThanOrEqualTo(1))));
    }
}
