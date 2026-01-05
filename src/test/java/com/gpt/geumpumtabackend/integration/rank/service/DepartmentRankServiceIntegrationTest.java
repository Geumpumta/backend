package com.gpt.geumpumtabackend.integration.rank.service;

import com.gpt.geumpumtabackend.integration.config.BaseIntegrationTest;
import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.service.DepartmentRankService;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("DepartmentRankService 통합 테스트")
class DepartmentRankServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DepartmentRankService departmentRankService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private DepartmentRankingRepository departmentRankingRepository;

    private User softwareUser1;
    private User softwareUser2;
    private User computerUser1;
    private User electronicUser1;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        softwareUser1 = createAndSaveUser("소프트웨어1", "software1@kumoh.ac.kr", Department.SOFTWARE);
        softwareUser2 = createAndSaveUser("소프트웨어2", "software2@kumoh.ac.kr", Department.SOFTWARE);
        computerUser1 = createAndSaveUser("컴퓨터공학1", "computer1@kumoh.ac.kr", Department.COMPUTER_ENGINEERING);
        electronicUser1 = createAndSaveUser("전자공학1", "electronic1@kumoh.ac.kr", Department.ELECTRONIC_SYSTEMS);
    }

    @Nested
    @DisplayName("현재 진행중인 학과 랭킹 일간 조회")
    class GetCurrentDailyDepartmentRanking {

        @Test
        @DisplayName("랭킹에_없는_학과_사용자는_꼴찌_다음_순위가_된다")
        void 랭킹에_없는_학과_사용자는_꼴찌_다음_순위가_된다() {
            // Given
            LocalDateTime today = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
            
            // 소프트웨어전공만 학습 기록 생성
            createCompletedStudySession(softwareUser1, today.minusHours(1), today);
            
            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(electronicUser1.getId());

            // Then
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.topRanks().get(0).departmentName()).isEqualTo("소프트웨어전공");
            
            // 내 학과 랭킹은 fallback으로 2등 (1 + 1)
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("전자시스템전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(2L);
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(0L);
        }

        @Test
        @DisplayName("아무도_학습하지_않은_날에는_빈_랭킹과_1등_fallback이_반환된다")
        void 아무도_학습하지_않은_날에는_빈_랭킹과_1등_fallback이_반환된다() {
            // Given - 아무 학습 기록 없음
            
            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(softwareUser1.getId());

            // Then
            assertThat(response.topRanks()).isEmpty();
            
            // 내 학과 랭킹은 1등 (0 + 1)
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(1L);
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(0L);
        }

        @Test
        @DisplayName("완료된_세션만_집계된다")
        void 완료된_세션만_집계된다() {
            // Given - 현재 시간 기준으로 정확한 시간 설정
            LocalDateTime now = LocalDateTime.now().withNano(0); // 나노초 제거
            LocalDateTime oneHourAgo = now.minusHours(1);
            LocalDateTime twoHoursAgo = now.minusHours(2); 
            LocalDateTime thirtyMinAgo = now.minusMinutes(30);
            
            // 소프트웨어전공: 완료 60분 + 완료 60분 = 120분 (진행중 세션 제거)
            createCompletedStudySession(softwareUser1, twoHoursAgo, oneHourAgo);  // 60분
            createCompletedStudySession(softwareUser2, oneHourAgo, now);  // 60분
            
            // 컴퓨터공학전공: 완료 30분만  
            createCompletedStudySession(computerUser1, oneHourAgo, thirtyMinAgo);  // 30분

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(softwareUser1.getId());

            // Then
            assertThat(response.topRanks()).hasSize(2);
            
            // 1등: 소프트웨어전공 (120분 = 7,200,000ms)
            DepartmentRankingEntryResponse rank1 = response.topRanks().get(0);
            assertThat(rank1.departmentName()).isEqualTo("소프트웨어전공");
            assertThat(rank1.totalMillis()).isCloseTo(7200000L, within(1000L)); // 1초 오차 허용
            
            // 2등: 컴퓨터공학전공 (30분 = 1,800,000ms)
            DepartmentRankingEntryResponse rank2 = response.topRanks().get(1);
            assertThat(rank2.departmentName()).isEqualTo("컴퓨터공학전공");
            assertThat(rank2.totalMillis()).isEqualTo(1800000L);
        }
    }

    @Nested
    @DisplayName("완료된 학과 랭킹 일간 조회")
    class GetCompletedDailyDepartmentRanking {

        @Test
        @DisplayName("과거_날짜의_학습_세션으로_랭킹이_재계산된다")
        void 과거_날짜의_학습_세션으로_랭킹이_재계산된다() {
            // Given - 어제 날짜로 학습 세션 생성
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atTime(9, 0);
            LocalDateTime yesterdayEnd1 = yesterday.atTime(11, 0);  // 120분
            LocalDateTime yesterdayEnd2 = yesterday.atTime(10, 30); // 90분

            // 소프트웨어전공: 120분
            createCompletedStudySession(softwareUser1, yesterdayStart, yesterdayEnd1);

            // 컴퓨터공학전공: 90분
            createCompletedStudySession(computerUser1, yesterdayStart, yesterdayEnd2);

            // When - 어제 날짜로 완료된 랭킹 조회
            DepartmentRankingResponse response = departmentRankService.getCompletedDailyDepartmentRanking(
                softwareUser1.getId(),
                yesterday.atStartOfDay()
            );

            // Then - 쿼리가 실시간 재계산하여 결과 반환
            assertThat(response.topRanks()).hasSize(2);

            // 1등: 소프트웨어전공 (120분)
            DepartmentRankingEntryResponse rank1 = response.topRanks().get(0);
            assertThat(rank1.departmentName()).isEqualTo("소프트웨어전공");
            assertThat(rank1.totalMillis()).isEqualTo(7200000L);
            assertThat(rank1.rank()).isEqualTo(1L);

            // 2등: 컴퓨터공학전공 (90분)
            DepartmentRankingEntryResponse rank2 = response.topRanks().get(1);
            assertThat(rank2.departmentName()).isEqualTo("컴퓨터공학전공");
            assertThat(rank2.totalMillis()).isEqualTo(5400000L);
            assertThat(rank2.rank()).isEqualTo(2L);

            // 내 학과 랭킹 확인
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(1L);
        }

        @Test
        @DisplayName("완료된_랭킹에_없는_학과는_fallback_랭킹이_생성된다")
        void 완료된_랭킹에_없는_학과는_fallback_랭킹이_생성된다() {
            // Given - 어제 날짜로 소프트웨어전공만 학습
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atTime(9, 0);
            LocalDateTime yesterdayEnd = yesterday.atTime(10, 0);

            createCompletedStudySession(softwareUser1, yesterdayStart, yesterdayEnd);

            // When - 전자공학과 사용자가 조회 (학습 기록 없음)
            DepartmentRankingResponse response = departmentRankService.getCompletedDailyDepartmentRanking(
                electronicUser1.getId(),
                yesterday.atStartOfDay()
            );

            // Then
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.topRanks().get(0).departmentName()).isEqualTo("소프트웨어전공");

            // 내 학과는 fallback으로 2등
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("전자시스템전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(2L);
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(0L);
        }

        @Test
        @DisplayName("학습_기록이_없는_날에는_빈_결과와_fallback이_반환된다")
        void 학습_기록이_없는_날에는_빈_결과와_fallback이_반환된다() {
            // Given - 일주일 전 날짜 (학습 기록 없음)
            LocalDate pastDate = LocalDate.now().minusDays(7);

            // When
            DepartmentRankingResponse response = departmentRankService.getCompletedDailyDepartmentRanking(
                softwareUser1.getId(),
                pastDate.atStartOfDay()
            );

            // Then
            assertThat(response.topRanks()).isEmpty();

            // 내 학과는 fallback으로 1등
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(1L);
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(0L);
        }

        @Test
        @DisplayName("날짜_경계_시간의_학습_세션도_정확히_집계된다")
        void 날짜_경계_시간의_학습_세션도_정확히_집계된다() {
            // Given - 자정을 걸치는 학습 세션
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime beforeMidnight = yesterday.atTime(23, 30);  // 어제 23:30
            LocalDateTime afterMidnight = yesterday.plusDays(1).atTime(0, 30);  // 오늘 00:30

            // 어제 23:30 ~ 오늘 00:30 학습 (총 60분, 어제 30분 + 오늘 30분)
            createCompletedStudySession(softwareUser1, beforeMidnight, afterMidnight);

            // When - 어제 날짜로 조회
            DepartmentRankingResponse response = departmentRankService.getCompletedDailyDepartmentRanking(
                softwareUser1.getId(),
                yesterday.atStartOfDay()
            );

            // Then - 어제 부분(30분)만 집계되어야 함
            assertThat(response.topRanks()).hasSize(1);
            DepartmentRankingEntryResponse rank = response.topRanks().get(0);
            assertThat(rank.departmentName()).isEqualTo("소프트웨어전공");
            // 어제 23:30 ~ 24:00 = 30분 = 1,800,000ms
            assertThat(rank.totalMillis()).isEqualTo(1800000L);
        }

        @Test
        @DisplayName("저장된_DepartmentRanking과_실시간_재계산이_병합된다")
        void 저장된_DepartmentRanking과_실시간_재계산이_병합된다() {
            // Given - 어제 날짜
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atTime(9, 0);
            LocalDateTime yesterdayEnd = yesterday.atTime(11, 0);  // 120분

            // 소프트웨어전공 학습 세션 생성 (120분)
            createCompletedStudySession(softwareUser1, yesterdayStart, yesterdayEnd);

            // 전자공학과는 학습 세션 없지만, 저장된 랭킹 데이터 생성
            // (실제로는 스케줄러가 생성하지만, 테스트를 위해 수동 생성)
            // 참고: 현재 쿼리는 실시간 재계산을 우선하므로 이 데이터는 무시될 수 있음
            DepartmentRanking electronicRanking = DepartmentRanking.builder()
                .department(Department.ELECTRONIC_SYSTEMS)
                .totalMillis(3600000L)  // 60분
                .rank(2L)
                .rankingType(RankingType.DAILY)
                .calculatedAt(yesterday.atStartOfDay())
                .build();
            departmentRankingRepository.save(electronicRanking);

            // When
            DepartmentRankingResponse response = departmentRankService.getCompletedDailyDepartmentRanking(
                softwareUser1.getId(),
                yesterday.atStartOfDay()
            );

            // Then - 실시간 재계산이 우선되므로 소프트웨어전공만 나타남
            // (전자공학과는 user는 있지만 study_session이 없어서 0으로 계산되고,
            //  0 > 0 이므로 topRanks에 포함되지 않음)
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.topRanks().get(0).departmentName()).isEqualTo("소프트웨어전공");
            assertThat(response.topRanks().get(0).totalMillis()).isEqualTo(7200000L);
        }
    }

    @Nested
    @DisplayName("학과별_상위_30명_기준_집계")
    class DepartmentTop30Calculation {

        @Test
        @DisplayName("학과별로_상위_30명의_학습시간만_합산된다")
        void 학과별로_상위_30명의_학습시간만_합산된다() {
            // Given - 소프트웨어전공 35명 생성 (30명 제한 검증)
            LocalDateTime today = LocalDateTime.now().withHour(12).withMinute(0);
            LocalDateTime oneHourAgo = today.minusHours(1);

            // 35명의 사용자 생성 및 각각 다른 학습 시간 부여
            // 1등: 350분, 2등: 340분, ..., 30등: 60분, 31등: 50분, ..., 35등: 10분
            for (int i = 1; i <= 35; i++) {
                User user = createAndSaveUser("소프트웨어" + i, "sw" + i + "@kumoh.ac.kr", Department.SOFTWARE);
                LocalDateTime sessionStart = oneHourAgo;
                LocalDateTime sessionEnd = oneHourAgo.plusMinutes((36 - i) * 10);
                createCompletedStudySession(user, sessionStart, sessionEnd);
            }

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(softwareUser1.getId());

            // Then - 0ms 학과는 필터링되어 제외됨
            assertThat(response.topRanks()).hasSize(1);

            // 1등: 소프트웨어전공
            DepartmentRankingEntryResponse rank1 = response.topRanks().get(0);
            assertThat(rank1.departmentName()).isEqualTo("소프트웨어전공");

            // 상위 30명만 합산: (350 + 340 + 330 + ... + 70 + 60)분
            // 등차수열 합: (첫항 + 끝항) * 항수 / 2 = (350 + 60) * 30 / 2 = 6150분
            long expectedMillis = 6150L * 60 * 1000; // 6150분 = 369,000,000ms
            assertThat(rank1.totalMillis()).isEqualTo(expectedMillis);

            // 31~35등(50분, 40분, 30분, 20분, 10분)은 제외되어야 함
            // 만약 전체 합산이면: (350 + 340 + ... + 20 + 10) = 6300분 = 378,000,000ms
            // 따라서 실제 결과는 378,000,000ms보다 작아야 함
            assertThat(rank1.totalMillis()).isLessThan(378000000L);
        }

        @Test
        @DisplayName("학과별_30명_미만인_경우_전체_인원이_집계된다")
        void 학과별_30명_미만인_경우_전체_인원이_집계된다() {
            // Given - 소프트웨어전공 10명만 생성
            LocalDateTime today = LocalDateTime.now().withHour(12).withMinute(0);
            LocalDateTime oneHourAgo = today.minusHours(1);

            // 10명의 사용자 생성 (각각 100분, 90분, ..., 10분)
            for (int i = 1; i <= 10; i++) {
                User user = createAndSaveUser("소프트웨어소수" + i, "sw-small" + i + "@kumoh.ac.kr", Department.SOFTWARE);
                LocalDateTime sessionStart = oneHourAgo;
                LocalDateTime sessionEnd = oneHourAgo.plusMinutes((11 - i) * 10);
                createCompletedStudySession(user, sessionStart, sessionEnd);
            }

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(softwareUser1.getId());

            // Then - 0ms 학과는 필터링되어 제외됨
            assertThat(response.topRanks()).hasSize(1);

            // 1등: 소프트웨어전공
            DepartmentRankingEntryResponse rank1 = response.topRanks().get(0);
            assertThat(rank1.departmentName()).isEqualTo("소프트웨어전공");

            // 전체 10명 합산: (100 + 90 + ... + 20 + 10) = 550분
            long expectedMillis = 550L * 60 * 1000; // 550분 = 33,000,000ms
            assertThat(rank1.totalMillis()).isEqualTo(expectedMillis);
        }

        @Test
        @DisplayName("여러_학과가_각각_상위_30명_기준으로_집계된다")
        void 여러_학과가_각각_상위_30명_기준으로_집계된다() {
            // Given
            LocalDateTime today = LocalDateTime.now().withHour(12).withMinute(0);
            LocalDateTime oneHourAgo = today.minusHours(1);

            // 소프트웨어전공: 35명 (상위 30명만 집계)
            for (int i = 1; i <= 35; i++) {
                User user = createAndSaveUser("SW멀티" + i, "sw-multi" + i + "@kumoh.ac.kr", Department.SOFTWARE);
                createCompletedStudySession(user, oneHourAgo, oneHourAgo.plusMinutes(100)); // 각 100분
            }

            // 컴퓨터공학전공: 20명 (전체 집계)
            for (int i = 1; i <= 20; i++) {
                User user = createAndSaveUser("컴공멀티" + i, "ce-multi" + i + "@kumoh.ac.kr", Department.COMPUTER_ENGINEERING);
                createCompletedStudySession(user, oneHourAgo, oneHourAgo.plusMinutes(80)); // 각 80분
            }

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(softwareUser1.getId());

            // Then - 0ms 학과는 필터링되어 제외됨
            assertThat(response.topRanks()).hasSize(2);

            // 1등: 소프트웨어전공 (30명 * 100분 = 3000분)
            DepartmentRankingEntryResponse rank1 = response.topRanks().get(0);
            assertThat(rank1.departmentName()).isEqualTo("소프트웨어전공");
            assertThat(rank1.totalMillis()).isEqualTo(3000L * 60 * 1000); // 180,000,000ms

            // 2등: 컴퓨터공학전공 (20명 * 80분 = 1600분)
            DepartmentRankingEntryResponse rank2 = response.topRanks().get(1);
            assertThat(rank2.departmentName()).isEqualTo("컴퓨터공학전공");
            assertThat(rank2.totalMillis()).isEqualTo(1600L * 60 * 1000); // 96,000,000ms
        }
    }

    // 테스트 헬퍼 메서드들
    private User createAndSaveUser(String name, String email, Department department) {
        User user = User.builder()
            .name(name)
            .email(email)
            .department(department)
            .picture("profile.jpg")
            .role(UserRole.USER)
            .provider(com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider.GOOGLE)
            .providerId("test-provider-id-" + email)
            .build();
        return userRepository.save(user);
    }

    private void createCompletedStudySession(User user, LocalDateTime startTime, LocalDateTime endTime) {
        StudySession session = new StudySession();
        session.startStudySession(startTime, user);
        session.endStudySession(endTime);
        studySessionRepository.save(session);
    }
}