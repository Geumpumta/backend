package com.gpt.geumpumtabackend.user.service;


import com.gpt.geumpumtabackend.fcm.service.FcmService;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.badge.dto.response.NewBadgeResponse;
import com.gpt.geumpumtabackend.badge.service.BadgeService;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.token.repository.RefreshTokenRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import com.gpt.geumpumtabackend.user.dto.request.NicknameVerifyRequest;
import com.gpt.geumpumtabackend.user.dto.request.ProfileUpdateRequest;
import com.gpt.geumpumtabackend.user.dto.response.CompleteRegistrationResponse;
import com.gpt.geumpumtabackend.user.dto.response.UserProfileResponse;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtHandler jwtHandler;
    private final FcmService fcmService;
    private final BadgeService badgeService;
    private static final Random RANDOM = new Random();

    private static final List<String> ADJECTIVES = List.of(
            "귀여운", "멋있는", "우아한", "깔끔한", "친절한", "유쾌한", "활발한", "따뜻한", "당당한", "섬세한", "냉철한", "순수한", "독특한", "정직한"
    );
    private static final List<String> NOUNS = List.of(
            "테크모", "금붕이", "까마귀", "직박구리", "도요새", "삼족오", "기러기", "쑥새", "소쩍새", "왜가리", "올빼미", "딱다구리", "뱁새", "개똥지빠귀"
    );
    private static final String DELETED_PREFIX = "deleted_";

    public boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));

        return user.getRole().equals(UserRole.ADMIN);
    }

    public void generateRandomNickname(User user){
        String nickname;
        do {
            nickname = ADJECTIVES.get(RANDOM.nextInt(ADJECTIVES.size())) + NOUNS.get(RANDOM.nextInt(NOUNS.size())) + RANDOM.nextInt(1, 101);
        }while(userRepository.existsByNickname(nickname));
        user.setInitialNickname(nickname);
    }

    @Transactional
    public CompleteRegistrationResponse completeRegistration(CompleteRegistrationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        validateDuplication(request);
        user.completeRegistration(request);
        generateRandomNickname(user);

        // 토큰 재발급
        JwtUserClaim jwtUserClaim = JwtUserClaim.create(user);
        Token token = jwtHandler.createTokens(jwtUserClaim);
        TokenResponse tokenResponse = TokenResponse.to(token);
        NewBadgeResponse newBadge = badgeService.grantWelcomeBadge(userId);
        return CompleteRegistrationResponse.of(tokenResponse, newBadge);
    }

    private void validateDuplication(CompleteRegistrationRequest request) {
        if(userRepository.existsBySchoolEmail((request.email()))){
            throw new BusinessException(ExceptionType.DUPLICATED_SCHOOL_EMAIL);
        }

        if(userRepository.existsByStudentId(request.studentId())){
            throw new BusinessException(ExceptionType.DUPLICATED_STUDENT_ID);
        }
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
    }

    public boolean isNicknameAvailable(NicknameVerifyRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));

        return !userRepository.existsByNickname(request.nickname());
    }

    @Transactional
    public void updateUserProfile(ProfileUpdateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));

        user.updateProfile(request.imageUrl(), request.publicId(), request.nickname());
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        refreshTokenRepository.deleteByUserId(userId);
        fcmService.removeFcmToken(userId);
    }

    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        refreshTokenRepository.deleteByUserId(userId);
        fcmService.removeFcmToken(userId);
        userRepository.deleteById(userId);
    }

    @Transactional
    public TokenResponse restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        String nickname = removeDeletedPrefix(user.getNickname());
        String email = removeDeletedPrefix(user.getEmail());
        String schoolEmail = removeDeletedPrefix(user.getSchoolEmail());
        String studentId = removeDeletedPrefix(user.getStudentId());
        user.restore(nickname, email, schoolEmail, studentId);

        JwtUserClaim jwtUserClaim = JwtUserClaim.create(user);
        Token token = jwtHandler.createTokens(jwtUserClaim);
        return TokenResponse.to(token);
    }

    public String removeDeletedPrefix(String value) {
        if (value == null) return null;
        return value.startsWith(DELETED_PREFIX)
                ? value.substring(DELETED_PREFIX.length())
                : value;
    }
}
