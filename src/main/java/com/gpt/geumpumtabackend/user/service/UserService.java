package com.gpt.geumpumtabackend.user.service;


import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeRequest;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));

        return user.getRole().equals(UserRole.ADMIN);
    }

    @Transactional
    public void saveSchoolEmail(Long userId, EmailCodeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        if(user.getSchoolEmail() != null)
            throw new BusinessException(ExceptionType.SCHOOL_EMAIL_ALREADY_REGISTERED);

        if(userRepository.findBySchoolEmail(request.email()))
            throw new BusinessException(ExceptionType.DUPLICATED_SCHOOL_EMAIL);

        user.registerSchoolEmail(request.email());
    }
}
