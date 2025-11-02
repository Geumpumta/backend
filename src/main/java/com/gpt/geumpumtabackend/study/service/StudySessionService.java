package com.gpt.geumpumtabackend.study.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.dto.request.StudyEndRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyReconnectRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudySessionResponse;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;

    /*
    메인 홈
     */
    public StudySessionResponse getTodayStudySession(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Long totalStudySession = studySessionRepository.sumCompletedStudySessionByUserId(userId, startOfDay, now);
        return StudySessionResponse.of(totalStudySession);
    }

    /*
    공부 시작
     */
    public StudyStartResponse startStudySession(StudyStartRequest request, Long userId) {
        // study_session_log 테이블에 새로운 행을 추가한다. 이때 start_time으로 넣고, end_time은 NULL로 설정한다.
        StudySession studySession = new StudySession();
        User user = userRepository.findById(userId)
                        .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        studySession.startStudySession(request.startTime(), user);
        return StudyStartResponse.fromEntity(studySessionRepository.save(studySession));
    }

    /*
    공부 종료
     */
    public void endStudySession(StudyEndRequest request, Long userId) {
        StudySession studysession = studySessionRepository.findByIdAndUser_Id(request.studySessionId(), userId)
                .orElseThrow(()->new BusinessException(ExceptionType.STUDY_SESSION_NOT_FOUND));
        studysession.endStudySession(request.endTime());
        studySessionRepository.save(studysession);
    }

    /*
    와이파이 끊겼을 시
     */
    @Transactional
    public StudyStartResponse reconnectStudySession(StudyReconnectRequest request, Long userId) {
        //  기존에 진행중이었던 세션의 endTime을 disconnectedTime으로 설정
        StudySession endStudySession = studySessionRepository.findByIdAndUser_Id(request.studySessionId(), userId)
                .orElseThrow(()->new BusinessException(ExceptionType.STUDY_SESSION_NOT_FOUND));
        endStudySession.endStudySession(request.disconnectedTime());
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        // 새로운 세션을 생성하여 재연결 시각을 통해 새로운 공부 시간 세션 INSERT
        StudySession newStudySession = new StudySession();
        newStudySession.startStudySession(request.reconnectTime(), user);
        return StudyStartResponse.fromEntity(studySessionRepository.save(newStudySession));
    }
}
