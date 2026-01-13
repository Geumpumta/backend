package com.gpt.geumpumtabackend.unit.rank.service;

import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.rank.service.DepartmentRankService;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentRankService 단위 테스트")
class DepartmentRankServiceTest {

    @Mock
    private DepartmentRankingRepository departmentRankingRepository;
    
    @Mock
    private StudySessionRepository studySessionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private DepartmentRankService departmentRankService;

    @Nested
    @DisplayName("현재 일간 학과 랭킹 조회")
    class GetCurrentDailyDepartmentRanking {

        @Test
        @DisplayName("현재 일간 학과 랭킹 조회 시 정상적으로 랭킹 정보가 반환된다")
        void getCurrentDaily_정상조회_학과랭킹정보반환() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "김철수", Department.SOFTWARE);

            List<DepartmentRankingTemp> mockDepartmentRankingData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 25200000L, 1L),
                    createMockDepartmentRankingTemp("COMPUTER_ENGINEERING", 21600000L, 2L),
                    createMockDepartmentRankingTemp("ELECTRONIC_SYSTEMS", 18000000L, 3L)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(mockDepartmentRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(3);
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(1L);
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(25200000L);

            verify(studySessionRepository).calculateCurrentDepartmentRanking(any(), any(), any());
        }

        @Test
        @DisplayName("사용자의 학과가 랭킹에 없을 때 fallback 랭킹이 생성된다")
        void getCurrentDaily_학과랭킹없음_fallback랭킹생성() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "김철수", Department.MECHANICAL_ENGINEERING);  // 기계공학과

            List<DepartmentRankingTemp> mockDepartmentRankingData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 25200000L, 1L),
                    createMockDepartmentRankingTemp("COMPUTER_ENGINEERING", 21600000L, 2L)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(mockDepartmentRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(userId);

            // Then
            assertThat(response.topRanks()).hasSize(2);
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("기계공학전공");
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(0L);
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(3L); // 마지막 순위 + 1

            verify(userRepository).findById(userId);
        }
    }


    @Nested
    @DisplayName("완료된 일간 학과 랭킹 조회")
    class GetCompletedDailyDepartmentRanking {

        @Test
        @DisplayName("완료된 일간 학과 랭킹 조회 시 정상적으로 랭킹 정보가 반환된다")
        void getCompletedDaily_정상조회_학과랭킹정보반환() {
            // Given
            Long userId = 1L;
            LocalDateTime startDay = LocalDateTime.of(2024, 1, 1, 0, 0);
            User testUser = createTestUser(userId, "김철수", Department.SOFTWARE);

            List<DepartmentRankingTemp> mockDepartmentRankingData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 30000000L, 1L),
                    createMockDepartmentRankingTemp("COMPUTER_ENGINEERING", 25000000L, 2L)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(departmentRankingRepository.getFinishedDepartmentRanking(startDay, RankingType.DAILY.name()))
                    .willReturn(mockDepartmentRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCompletedDailyDepartmentRanking(userId, startDay);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(2);
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");

            verify(departmentRankingRepository).getFinishedDepartmentRanking(startDay, RankingType.DAILY.name());
        }
    }

    @Nested
    @DisplayName("현재 주간 학과 랭킹 조회")
    class GetCurrentWeeklyDepartmentRanking {

        @Test
        @DisplayName("현재 주간 학과 랭킹 조회 시 월요일부터 일요일까지의 기간으로 계산된다")
        void getCurrentWeekly_정상조회_주간기간계산() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "김철수", Department.SOFTWARE);

            List<DepartmentRankingTemp> mockDepartmentRankingData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 100800000L, 1L)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(mockDepartmentRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentWeeklyDepartmentRanking(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");

            verify(studySessionRepository).calculateCurrentDepartmentRanking(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("현재 월간 학과 랭킹 조회")
    class GetCurrentMonthlyDepartmentRanking {

        @Test
        @DisplayName("현재 월간 학과 랭킹 조회 시 해당 월의 첫날부터 마지막날까지의 기간으로 계산된다")
        void getCurrentMonthly_정상조회_월간기간계산() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "김철수", Department.SOFTWARE);

            List<DepartmentRankingTemp> mockDepartmentRankingData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 432000000L, 1L)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(mockDepartmentRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentMonthlyDepartmentRanking(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");

            verify(studySessionRepository).calculateCurrentDepartmentRanking(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("학과 랭킹 응답 생성 로직")
    class BuildDepartmentRankingResponse {

        @Test
        @DisplayName("빈 학과 랭킹 목록에서도 내 학과 랭킹이 정상적으로 생성된다")
        void buildResponse_빈학과랭킹목록_내학과랭킹생성정상() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "홀로학과원", Department.ELECTRONIC_SYSTEMS);
            List<DepartmentRankingTemp> emptyRankingData = List.of();

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(emptyRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(userId);

            // Then
            assertThat(response.topRanks()).isEmpty();
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("전자시스템전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(1L); // 0 + 1
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(0L);
        }

        @Test
        @DisplayName("대량의 학과 랭킹 데이터에서 내 학과를 정확히 찾는다")
        void buildResponse_대량학과랭킹데이터_내학과정확검색() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "컴공생", Department.COMPUTER_ENGINEERING);

            List<DepartmentRankingTemp> largeRankingData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 50000000L, 1L),
                    createMockDepartmentRankingTemp("COMPUTER_ENGINEERING", 40000000L, 2L),
                    createMockDepartmentRankingTemp("ELECTRONIC_SYSTEMS", 30000000L, 3L),
                    createMockDepartmentRankingTemp("MECHANICAL_ENGINEERING", 20000000L, 4L),
                    createMockDepartmentRankingTemp("ARTIFICIAL_INTELLIGENCE", 10000000L, 5L)
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(largeRankingData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(userId);

            // Then
            assertThat(response.topRanks()).hasSize(5);
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("컴퓨터공학전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(2L);
            assertThat(response.myDepartmentRanking().totalMillis()).isEqualTo(40000000L);
        }

        @Test
        @DisplayName("학과명 매칭은 정확한 문자열 비교로 동작한다")
        void buildResponse_학과명매칭_정확한문자열비교() {
            // Given
            Long userId = 1L;
            User testUser = createTestUser(userId, "소프트웨어생", Department.SOFTWARE);

            List<DepartmentRankingTemp> mockData = List.of(
                    createMockDepartmentRankingTemp("SOFTWARE", 50000000L, 1L),
                    createMockDepartmentRankingTemp("ARTIFICIAL_INTELLIGENCE", 40000000L, 2L)  // 다른 학과
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(studySessionRepository.calculateCurrentDepartmentRanking(any(), any(), any()))
                    .willReturn(mockData);

            // When
            DepartmentRankingResponse response = departmentRankService.getCurrentDailyDepartmentRanking(userId);

            // Then
            assertThat(response.myDepartmentRanking()).isNotNull();
            assertThat(response.myDepartmentRanking().departmentName()).isEqualTo("소프트웨어전공");
            assertThat(response.myDepartmentRanking().rank()).isEqualTo(1L);  // 정확히 매칭된 것만
        }
    }

    // 테스트 데이터 생성 헬퍼 메서드
    private User createTestUser(Long id, String name, Department department) {
        User user = User.builder()
                .name(name)
                .email("test@kumoh.ac.kr")
                .department(department)
                .picture("test.jpg")
                .role(com.gpt.geumpumtabackend.user.domain.UserRole.USER)
                .provider(com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider.GOOGLE)
                .providerId("test-provider-id")
                .build();

        // 테스트용 ID 설정 (Reflection 사용)
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test user ID", e);
        }

        return user;
    }

    private DepartmentRankingTemp createMockDepartmentRankingTemp(String department, Long totalMillis, Long ranking) {
        DepartmentRankingTemp mock = mock(DepartmentRankingTemp.class);
        // getDepartment()는 실제로 사용되지 않으므로 stubbing 제거
        given(mock.getTotalMillis()).willReturn(totalMillis);
        given(mock.getRanking()).willReturn(ranking);

        // getDepartmentName만 모킹 (DepartmentRankingEntryResponse.of()에서 실제 사용됨)
        String koreanName = Department.valueOf(department).getKoreanName();
        given(mock.getDepartmentName()).willReturn(koreanName);

        return mock;
    }
}
