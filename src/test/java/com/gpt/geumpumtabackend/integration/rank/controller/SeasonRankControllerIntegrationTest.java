package com.gpt.geumpumtabackend.integration.rank.controller;

import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.rank.domain.*;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.repository.SeasonRankingSnapshotRepository;
import com.gpt.geumpumtabackend.rank.repository.SeasonRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SeasonRank Controller 통합 테스트")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
class SeasonRankControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtHandler jwtHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private UserRankingRepository userRankingRepository;

    @Autowired
    private DepartmentRankingRepository departmentRankingRepository;

    @Autowired
    private SeasonRankingSnapshotRepository snapshotRepository;

    private User softwareUser1;
    private User softwareUser2;
    private User computerUser;
    private String softwareUserToken;
    private Season activeSeason;

    @BeforeEach
    void setUp() {
        softwareUser1 = createUser("소프트웨어1", "sw1@kumoh.ac.kr", Department.SOFTWARE);
        softwareUser2 = createUser("소프트웨어2", "sw2@kumoh.ac.kr", Department.SOFTWARE);
        computerUser = createUser("컴퓨터공학", "ce@kumoh.ac.kr", Department.COMPUTER_ENGINEERING);

        JwtUserClaim claim = new JwtUserClaim(softwareUser1.getId(), UserRole.USER, false);
        Token token = jwtHandler.createTokens(claim);
        softwareUserToken = token.getAccessToken();

        // 현재 날짜가 포함되는 ACTIVE 시즌 생성
        LocalDate today = LocalDate.now();
        LocalDate seasonStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate seasonEnd = today.plusMonths(2).withDayOfMonth(1).minusDays(1);

        activeSeason = seasonRepository.save(Season.builder()
            .name("테스트 시즌")
            .seasonType(SeasonType.WINTER_VACATION)
            .startDate(seasonStart)
            .endDate(seasonEnd)
            .status(SeasonStatus.ACTIVE)
            .build());
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
    @DisplayName("현재 시즌 전체 랭킹 API")
    class GetCurrentSeasonRanking {

        @Test
        @DisplayName("현재_시즌_전체_랭킹을_조회한다")
        void 현재_시즌_전체_랭킹을_조회한다() throws Exception {
            // Given - 오늘의 학습 기록
            LocalDateTime today = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
            createStudySession(softwareUser1, today, 3);
            createStudySession(softwareUser2, today, 2);
            createStudySession(computerUser, today, 1);

            // When & Then
            mockMvc.perform(get("/api/v1/rank/season/current")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.seasonId").value(activeSeason.getId()))
                    .andExpect(jsonPath("$.data.seasonName").value("테스트 시즌"))
                    .andExpect(jsonPath("$.data.rankings").isArray())
                    .andExpect(jsonPath("$.data.rankings", hasSize(3)))
                    .andExpect(jsonPath("$.data.rankings[0].rank").value(1))
                    .andExpect(jsonPath("$.data.rankings[0].totalMillis").value(greaterThan(0)));
        }

        @Test
        @DisplayName("인증_없이_요청하면_403_에러가_발생한다")
        void 인증_없이_요청하면_403_에러가_발생한다() throws Exception {
            mockMvc.perform(get("/api/v1/rank/season/current"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("현재 시즌 학과 랭킹 API")
    class GetCurrentSeasonDepartmentRanking {

        @Test
        @DisplayName("현재_시즌_학과_집계_랭킹을_조회한다")
        void 현재_시즌_학과_집계_랭킹을_조회한다() throws Exception {
            // Given - 오늘의 학습 기록
            LocalDateTime today = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
            createStudySession(softwareUser1, today, 3); // SOFTWARE: 3시간
            createStudySession(softwareUser2, today, 2); // SOFTWARE: 2시간 (총 5시간)
            createStudySession(computerUser, today, 1);   // COMPUTER_ENGINEERING: 1시간

            // When & Then
            mockMvc.perform(get("/api/v1/rank/season/current/department")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.seasonId").value(activeSeason.getId()))
                    .andExpect(jsonPath("$.data.topRanks").isArray())
                    .andExpect(jsonPath("$.data.topRanks", hasSize(2)))
                    .andExpect(jsonPath("$.data.topRanks[0].departmentName").value("소프트웨어전공"))
                    .andExpect(jsonPath("$.data.topRanks[0].rank").value(1))
                    .andExpect(jsonPath("$.data.topRanks[1].departmentName").value("컴퓨터공학전공"))
                    .andExpect(jsonPath("$.data.topRanks[1].rank").value(2))
                    .andExpect(jsonPath("$.data.myDepartmentRanking").exists())
                    .andExpect(jsonPath("$.data.myDepartmentRanking.departmentName").value("소프트웨어전공"))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.rank").value(1));
        }
    }

    @Nested
    @DisplayName("종료된 시즌 전체 랭킹 API")
    class GetEndedSeasonRanking {

        @Test
        @DisplayName("종료된_시즌_전체_랭킹을_조회한다")
        void 종료된_시즌_전체_랭킹을_조회한다() throws Exception {
            // Given - 종료된 시즌 생성
            Season endedSeason = seasonRepository.save(Season.builder()
                .name("2023 2학기 시즌")
                .seasonType(SeasonType.FALL_SEMESTER)
                .startDate(LocalDate.of(2023, 9, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .status(SeasonStatus.ENDED)
                .build());

            // 스냅샷 데이터 생성
            snapshotRepository.save(SeasonRankingSnapshot.builder()
                .seasonId(endedSeason.getId())
                .userId(softwareUser1.getId())
                .rankType(RankType.OVERALL)
                .finalRank(1)
                .finalTotalMillis(10000000L)
                .department(Department.SOFTWARE)
                .snapshotAt(LocalDateTime.now())
                .build());

            snapshotRepository.save(SeasonRankingSnapshot.builder()
                .seasonId(endedSeason.getId())
                .userId(computerUser.getId())
                .rankType(RankType.OVERALL)
                .finalRank(2)
                .finalTotalMillis(8000000L)
                .department(Department.COMPUTER_ENGINEERING)
                .snapshotAt(LocalDateTime.now())
                .build());

            // When & Then
            mockMvc.perform(get("/api/v1/rank/season/" + endedSeason.getId())
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.seasonId").value(endedSeason.getId()))
                    .andExpect(jsonPath("$.data.rankings", hasSize(2)))
                    .andExpect(jsonPath("$.data.rankings[0].rank").value(1))
                    .andExpect(jsonPath("$.data.rankings[0].totalMillis").value(10000000));
        }

        @Test
        @DisplayName("존재하지_않는_시즌_조회시_SEASON_NOT_FOUND_에러가_발생한다")
        void 존재하지_않는_시즌_조회시_SEASON_NOT_FOUND_에러가_발생한다() throws Exception {
            mockMvc.perform(get("/api/v1/rank/season/99999")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value("false"));
        }

        @Test
        @DisplayName("ACTIVE_시즌에_종료_API_요청시_SEASON_NOT_ENDED_에러가_발생한다")
        void ACTIVE_시즌에_종료_API_요청시_SEASON_NOT_ENDED_에러가_발생한다() throws Exception {
            mockMvc.perform(get("/api/v1/rank/season/" + activeSeason.getId())
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value("false"));
        }
    }

    @Nested
    @DisplayName("종료된 시즌 학과 랭킹 API")
    class GetEndedSeasonDepartmentRanking {

        @Test
        @DisplayName("종료된_시즌_학과_집계_랭킹을_조회한다")
        void 종료된_시즌_학과_집계_랭킹을_조회한다() throws Exception {
            // Given - 종료된 시즌 생성
            Season endedSeason = seasonRepository.save(Season.builder()
                .name("2023 2학기 시즌")
                .seasonType(SeasonType.FALL_SEMESTER)
                .startDate(LocalDate.of(2023, 9, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .status(SeasonStatus.ENDED)
                .build());

            // DEPARTMENT 타입 스냅샷 생성 (학과별 상위 30명 개인 데이터)
            // SOFTWARE 학과: 2명
            snapshotRepository.save(SeasonRankingSnapshot.builder()
                .seasonId(endedSeason.getId())
                .userId(softwareUser1.getId())
                .rankType(RankType.DEPARTMENT)
                .finalRank(1)
                .finalTotalMillis(10000000L)
                .department(Department.SOFTWARE)
                .snapshotAt(LocalDateTime.now())
                .build());
            snapshotRepository.save(SeasonRankingSnapshot.builder()
                .seasonId(endedSeason.getId())
                .userId(softwareUser2.getId())
                .rankType(RankType.DEPARTMENT)
                .finalRank(2)
                .finalTotalMillis(8000000L)
                .department(Department.SOFTWARE)
                .snapshotAt(LocalDateTime.now())
                .build());

            // COMPUTER_ENGINEERING 학과: 1명
            snapshotRepository.save(SeasonRankingSnapshot.builder()
                .seasonId(endedSeason.getId())
                .userId(computerUser.getId())
                .rankType(RankType.DEPARTMENT)
                .finalRank(1)
                .finalTotalMillis(9000000L)
                .department(Department.COMPUTER_ENGINEERING)
                .snapshotAt(LocalDateTime.now())
                .build());

            // When & Then
            // SOFTWARE: 10000000 + 8000000 = 18000000 (1등)
            // COMPUTER_ENGINEERING: 9000000 (2등)
            mockMvc.perform(get("/api/v1/rank/season/" + endedSeason.getId() + "/department")
                            .header("Authorization", "Bearer " + softwareUserToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("true"))
                    .andExpect(jsonPath("$.data.seasonId").value(endedSeason.getId()))
                    .andExpect(jsonPath("$.data.topRanks", hasSize(2)))
                    .andExpect(jsonPath("$.data.topRanks[0].departmentName").value("소프트웨어전공"))
                    .andExpect(jsonPath("$.data.topRanks[0].totalMillis").value(18000000))
                    .andExpect(jsonPath("$.data.topRanks[0].rank").value(1))
                    .andExpect(jsonPath("$.data.topRanks[1].departmentName").value("컴퓨터공학전공"))
                    .andExpect(jsonPath("$.data.topRanks[1].totalMillis").value(9000000))
                    .andExpect(jsonPath("$.data.topRanks[1].rank").value(2))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.departmentName").value("소프트웨어전공"))
                    .andExpect(jsonPath("$.data.myDepartmentRanking.rank").value(1));
        }
    }
}
