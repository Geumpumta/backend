package com.gpt.geumpumtabackend.user.service;


import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
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
    private final JwtHandler jwtHandler;
    private static final Random RANDOM = new Random();

    private static final List<String> ADJECTIVES = List.of(
            "귀여운", "멋있는", "우아한", "깔끔한", "친절한", "유쾌한", "활발한", "따뜻한", "당당한", "섬세한", "냉철한", "순수한", "독특한", "정직한"
    );
    private static final List<String> NOUNS = List.of(
            "테크모", "금붕이", "까마귀", "직박구리", "도요새", "삼족오", "기러기", "쑥새", "소쩍새", "왜가리", "올빼미", "딱다구리", "뱁새", "개똥지빠귀"
    );

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
    public TokenResponse completeRegistration(CompleteRegistrationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        user.completeRegistration(request);
        generateRandomNickname(user);

        // 토큰 재발급
        JwtUserClaim jwtUserClaim = JwtUserClaim.create(user);
        Token token = jwtHandler.createTokens(jwtUserClaim);
        return TokenResponse.to(token);
    }
}
