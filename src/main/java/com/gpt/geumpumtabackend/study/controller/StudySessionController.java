package com.gpt.geumpumtabackend.study.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.study.api.StudySessionApi;
import com.gpt.geumpumtabackend.study.dto.request.StudyEndRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudySessionResponse;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import com.gpt.geumpumtabackend.study.service.StudySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/study")
@RequiredArgsConstructor
@Slf4j
public class StudySessionController implements StudySessionApi {

    private final StudySessionService studySessionService;

    /*
    메인 홈
     */
    @GetMapping
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<StudySessionResponse>> getTodayStudySession(Long userId){
        StudySessionResponse studySessionResponse = studySessionService.getTodayStudySession(userId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(studySessionResponse));
    }

    /*
    공부 시작
     */
    @PostMapping("/start")
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<StudyStartResponse>> startStudySession(@Valid @RequestBody StudyStartRequest request,
                                                                              Long userId){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(studySessionService.startStudySession(request, userId)));
    }

    /*
    공부 종료
     */
    @PostMapping("/end")
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<Void>> endStudySession(@Valid @RequestBody StudyEndRequest request, Long userId){
        studySessionService.endStudySession(request, userId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }
}
