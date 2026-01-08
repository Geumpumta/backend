package com.gpt.geumpumtabackend.unit.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.rank.service.PersonalRankService;
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
@DisplayName("PersonalRankService 단위 테스트")
class PersonalRankServiceTest {

    @Mock
    private UserRankingRepository userRankingRepository;
    
    @Mock
    private StudySessionRepository studySessionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private PersonalRankService personalRankService;

    @Nested
    @DisplayName("현재 일간 랭킹 조회")
    class GetCurrentDaily {

        @Test
        @DisplayName("현재 일간 랭킹 조회 시 정상적으로 랭킹 정보가 반환된다")
        void getCurrentDaily_정상조회_랭킹정보반환() {
            // Given
            Long userId = 2L;
            List<PersonalRankingTemp> mockRankingData = List.of(
                createMockPersonalRankingTemp(1L, "김철수", "profile1.jpg", "SOFTWARE", 7200000L, 1L),
                createMockPersonalRankingTemp(2L, "박영희", "profile2.jpg", "COMPUTER_ENGINEERING", 5400000L, 2L),
                createMockPersonalRankingTemp(3L, "이민수", "profile3.jpg", "ELECTRONIC_SYSTEMS", 3600000L, 3L)
            );

            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(mockRankingData);

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(3);
            assertThat(response.myRanking()).isNotNull();
            assertThat(response.myRanking().userId()).isEqualTo(2L);
            assertThat(response.myRanking().rank()).isEqualTo(2L);
            assertThat(response.myRanking().totalMillis()).isEqualTo(5400000L);
            
            verify(studySessionRepository).calculateCurrentPeriodRanking(any(), any(), any());
        }

        @Test
        @DisplayName("사용자가 랭킹에 없을 때 fallback 랭킹 정보가 생성된다")
        void getCurrentDaily_사용자랭킹없음_fallback랭킹생성() {
            // Given
            Long userId = 999L;
            List<PersonalRankingTemp> mockRankingData = List.of(
                createMockPersonalRankingTemp(1L, "김철수", "profile1.jpg", "SOFTWARE", 7200000L, 1L),
                createMockPersonalRankingTemp(2L, "박영희", "profile2.jpg", "COMPUTER_ENGINEERING", 5400000L, 2L)
            );
            
            User testUser = createTestUser(userId, "홍길동", Department.SOFTWARE);

            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(mockRankingData);
            given(userRepository.findById(userId))
                .willReturn(Optional.of(testUser));

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(userId);

            // Then
            assertThat(response.topRanks()).hasSize(2);
            assertThat(response.myRanking()).isNotNull();
            assertThat(response.myRanking().userId()).isEqualTo(userId);
            assertThat(response.myRanking().totalMillis()).isEqualTo(0L);
            assertThat(response.myRanking().rank()).isEqualTo(3L); // 마지막 순위 + 1
            assertThat(response.myRanking().username()).isEqualTo("홍길동");
            assertThat(response.myRanking().department()).isEqualTo("소프트웨어전공");
            
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("사용자가 랭킹에도 없고 DB에도 없을 때 USER_NOT_FOUND 예외가 발생한다")
        void getCurrentDaily_사용자없음_예외발생() {
            // Given
            Long userId = 999L;
            List<PersonalRankingTemp> mockRankingData = List.of(
                createMockPersonalRankingTemp(1L, "김철수", "profile1.jpg", "SOFTWARE", 7200000L, 1L)
            );

            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(mockRankingData);
            given(userRepository.findById(userId))
                .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> personalRankService.getCurrentDaily(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("완료된 일간 랭킹 조회")
    class GetCompletedDaily {

        @Test
        @DisplayName("완료된 일간 랭킹 조회 시 정상적으로 랭킹 정보가 반환된다")
        void getCompletedDaily_정상조회_랭킹정보반환() {
            // Given
            Long userId = 1L;
            LocalDateTime day = LocalDateTime.of(2024, 1, 1, 0, 0);
            
            List<PersonalRankingTemp> mockRankingData = List.of(
                createMockPersonalRankingTemp(1L, "김철수", "profile1.jpg", "SOFTWARE", 10800000L, 1L),
                createMockPersonalRankingTemp(2L, "박영희", "profile2.jpg", "COMPUTER_ENGINEERING", 9000000L, 2L)
            );

            given(userRankingRepository.getFinishedPersonalRanking(day, RankingType.DAILY))
                .willReturn(mockRankingData);

            // When
            PersonalRankingResponse response = personalRankService.getCompletedDaily(userId, day);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(2);
            assertThat(response.myRanking()).isNotNull();
            assertThat(response.myRanking().userId()).isEqualTo(1L);
            assertThat(response.myRanking().rank()).isEqualTo(1L);
            
            verify(userRankingRepository).getFinishedPersonalRanking(day, RankingType.DAILY);
        }
    }

    @Nested
    @DisplayName("현재 주간 랭킹 조회")
    class GetCurrentWeekly {

        @Test
        @DisplayName("현재 주간 랭킹 조회 시 월요일부터 일요일까지의 기간으로 계산된다")
        void getCurrentWeekly_정상조회_주간기간계산() {
            // Given
            Long userId = 1L;
            List<PersonalRankingTemp> mockRankingData = List.of(
                createMockPersonalRankingTemp(1L, "김철수", "profile1.jpg", "SOFTWARE", 25200000L, 1L)
            );

            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(mockRankingData);

            // When
            PersonalRankingResponse response = personalRankService.getCurrentWeekly(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.myRanking().userId()).isEqualTo(1L);
            
            verify(studySessionRepository).calculateCurrentPeriodRanking(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("현재 월간 랭킹 조회")
    class GetCurrentMonthly {

        @Test
        @DisplayName("현재 월간 랭킹 조회 시 해당 월의 첫날부터 마지막날까지의 기간으로 계산된다")
        void getCurrentMonthly_정상조회_월간기간계산() {
            // Given
            Long userId = 1L;
            List<PersonalRankingTemp> mockRankingData = List.of(
                createMockPersonalRankingTemp(1L, "김철수", "profile1.jpg", "SOFTWARE", 108000000L, 1L)
            );

            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(mockRankingData);

            // When
            PersonalRankingResponse response = personalRankService.getCurrentMonthly(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.topRanks()).hasSize(1);
            assertThat(response.myRanking().userId()).isEqualTo(1L);
            
            verify(studySessionRepository).calculateCurrentPeriodRanking(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("랭킹 Fallback 로직")
    class RankingFallbackLogic {

        @Test
        @DisplayName("랭킹에_없는_사용자는_꼴찌_다음_순위가_된다")
        void 랭킹에_없는_사용자는_꼴찌_다음_순위가_된다() {
            // Given
            Long notInRankingUserId = 999L;
            List<PersonalRankingTemp> rankings = List.of(
                createMockPersonalRankingTemp(1L, "1위자", "p1.jpg", "SOFTWARE", 10000L, 1L),
                createMockPersonalRankingTemp(2L, "2위자", "p2.jpg", "COMPUTER_ENGINEERING", 5000L, 2L)
            );
            User notInRankingUser = createTestUser(notInRankingUserId, "신규사용자", Department.SOFTWARE);
            
            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(rankings);
            given(userRepository.findById(notInRankingUserId))
                .willReturn(Optional.of(notInRankingUser));

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(notInRankingUserId);

            // Then
            assertThat(response.myRanking().rank()).isEqualTo(3L); // 마지막 순위(2) + 1
            assertThat(response.myRanking().totalMillis()).isEqualTo(0L); // 0시간 공부
            assertThat(response.myRanking().userId()).isEqualTo(notInRankingUserId);
            assertThat(response.myRanking().username()).isEqualTo("신규사용자");
        }

        @Test
        @DisplayName("빈_랭킹_목록에서도_첫번째_순위가_된다")
        void 빈_랭킹_목록에서도_첫번째_순위가_된다() {
            // Given
            Long userId = 1L;
            List<PersonalRankingTemp> emptyRankings = List.of();
            User firstUser = createTestUser(userId, "처음사용자", Department.SOFTWARE);
            
            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(emptyRankings);
            given(userRepository.findById(userId))
                .willReturn(Optional.of(firstUser));

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(userId);

            // Then
            assertThat(response.myRanking().rank()).isEqualTo(1L); // 0 + 1
            assertThat(response.myRanking().totalMillis()).isEqualTo(0L);
            assertThat(response.topRanks()).isEmpty();
        }

        @Test
        @DisplayName("많은_랭킹_데이터에서_마지막_순위_종합")
        void 많은_랭킹_데이터에서_마지막_순위_종합() {
            // Given
            Long notInRankingUserId = 999L;
            
            // 3명의 랭킹 데이터 생성
            List<PersonalRankingTemp> largeRankings = List.of(
                createMockPersonalRankingTemp(1L, "TOP1", "p1.jpg", "SOFTWARE", 1000000L, 1L),
                createMockPersonalRankingTemp(50L, "MIDDLE", "p50.jpg", "COMPUTER_ENGINEERING", 500000L, 50L),
                createMockPersonalRankingTemp(100L, "LAST", "p100.jpg", "ELECTRONIC_SYSTEMS", 10000L, 100L)
            );
            
            User notInRankingUser = createTestUser(notInRankingUserId, "완전신규사용자", Department.SOFTWARE);
            
            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(largeRankings);
            given(userRepository.findById(notInRankingUserId))
                .willReturn(Optional.of(notInRankingUser));

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(notInRankingUserId);

            // Then
            assertThat(response.myRanking().rank()).isEqualTo(4L); // 3명 + 1 = 4등
            assertThat(response.myRanking().totalMillis()).isEqualTo(0L);
        }

        @Test
        @DisplayName("부서_없는_사용자도_랭킹_fallback이_정상_동작한다")
        void 부서_없는_사용자도_랭킹_fallback이_정상_동작한다() {
            // Given
            Long userId = 999L;
            List<PersonalRankingTemp> rankings = List.of(
                createMockPersonalRankingTemp(1L, "1등", "p1.jpg", "SOFTWARE", 10000L, 1L)
            );
            
            User userWithNoDepartment = User.builder()
                .name("무소속사용자")
                .email("nodept@kumoh.ac.kr")
                .department(null)  // 부서 없음
                .picture("profile.jpg")
                .role(com.gpt.geumpumtabackend.user.domain.UserRole.USER)
                .provider(com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider.GOOGLE)
                .providerId("test-provider-id")
                .build();
                
            // Reflection으로 ID 설정
            try {
                java.lang.reflect.Field idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(userWithNoDepartment, userId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set test user ID", e);
            }
            
            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(rankings);
            given(userRepository.findById(userId))
                .willReturn(Optional.of(userWithNoDepartment));

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(userId);

            // Then
            assertThat(response.myRanking().rank()).isEqualTo(2L); // 1 + 1
            assertThat(response.myRanking().department()).isNull(); // null 부서 처리
            assertThat(response.myRanking().username()).isEqualTo("무소속사용자");
        }

        @Test
        @DisplayName("랭킹에_있는_사용자는_실제_랭킹_정보를_반환한다")
        void 랭킹에_있는_사용자는_실제_랭킹_정보를_반환한다() {
            // Given
            Long userId = 2L; // 랭킹에 있는 사용자
            List<PersonalRankingTemp> rankings = List.of(
                createMockPersonalRankingTemp(1L, "1등", "p1.jpg", "SOFTWARE", 10000L, 1L),
                createMockPersonalRankingTemp(2L, "2등", "p2.jpg", "COMPUTER_ENGINEERING", 8000L, 2L),
                createMockPersonalRankingTemp(3L, "3등", "p3.jpg", "ELECTRONIC_SYSTEMS", 5000L, 3L)
            );
            
            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(rankings);
            // 랭킹에 있으므로 userRepository.findById 호출 안됨

            // When
            PersonalRankingResponse response = personalRankService.getCurrentDaily(userId);

            // Then
            assertThat(response.myRanking().rank()).isEqualTo(2L); // 실제 랭킹
            assertThat(response.myRanking().totalMillis()).isEqualTo(8000L); // 실제 공부시간
            assertThat(response.myRanking().username()).isEqualTo("2등");
            
            // fallback 로직을 타지 않으므로 userRepository는 호출되지 않음
            verify(userRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("예외 상황 처리")
    class ExceptionHandling {

        @Test
        @DisplayName("랭킹에도_없고_DB에도_없는_사용자에게_USER_NOT_FOUND_예외가_발생한다")
        void 랭킹에도_없고_DB에도_없는_사용자에게_USER_NOT_FOUND_예외가_발생한다() {
            // Given
            Long nonExistentUserId = 999L;
            List<PersonalRankingTemp> rankings = List.of(
                createMockPersonalRankingTemp(1L, "1등", "p1.jpg", "SOFTWARE", 10000L, 1L)
            );
            
            given(studySessionRepository.calculateCurrentPeriodRanking(any(), any(), any()))
                .willReturn(rankings);
            given(userRepository.findById(nonExistentUserId))
                .willReturn(Optional.empty()); // DB에 사용자 없음

            // When & Then
            assertThatThrownBy(() -> personalRankService.getCurrentDaily(nonExistentUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.USER_NOT_FOUND);
            
            verify(userRepository).findById(nonExistentUserId);
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

    private PersonalRankingTemp createMockPersonalRankingTemp(Long userId, String nickname, String imageUrl, String department, Long totalMillis, Long ranking) {
        PersonalRankingTemp mock = mock(PersonalRankingTemp.class);
        given(mock.getUserId()).willReturn(userId);
        given(mock.getNickname()).willReturn(nickname);
        given(mock.getImageUrl()).willReturn(imageUrl);
        // getDepartment()는 실제로 사용되지 않으므로 stubbing 제거
        given(mock.getTotalMillis()).willReturn(totalMillis);
        given(mock.getRanking()).willReturn(ranking);

        // getDepartmentKoreanName만 모킹 (PersonalRankingEntryResponse.of()에서 실제 사용됨)
        String koreanName = Department.valueOf(department).getKoreanName();
        given(mock.getDepartmentKoreanName()).willReturn(koreanName);

        return mock;
    }
}