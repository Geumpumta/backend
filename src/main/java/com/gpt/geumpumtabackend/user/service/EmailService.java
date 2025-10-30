package com.gpt.geumpumtabackend.user.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeRequest;
import com.gpt.geumpumtabackend.user.dto.request.EmailCodeVerifyRequest;
import com.gpt.geumpumtabackend.user.dto.response.EmailCodeVerifyResponse;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String username;



    // TODO : 인증코드 검증과 삭제를 원자적으로 수행해야함
    // 인증코드 검증
    public EmailCodeVerifyResponse verifyCode(EmailCodeVerifyRequest request, Long userId){
        if(isEmailOwner(request, userId)){
            return EmailCodeVerifyResponse.of(false);
        }
        String emailCode = redisTemplate.opsForValue().get("email:"+request.email());
        if(emailCode == null){
            return EmailCodeVerifyResponse.of(false);
        }
        if(emailCode.equals(request.code())){
            deleteCode(request.email());
            return EmailCodeVerifyResponse.of(true);
        }
        return EmailCodeVerifyResponse.of(false);
    }

    // redis에서 인증코드 삭제
    public void deleteCode(String email){
        redisTemplate.delete("email:"+email);
    }

    // 메일 전송
    public void sendMail(EmailCodeRequest request){
        try{
            String code = createCode();
            MimeMessage mimeMessage = createMimeMessage(request, code);

            // TODO: 하드코딩된 key를 변경하기
            redisTemplate.opsForValue().set(
                    "email:" + request.email(),
                    code,
                    5, TimeUnit.MINUTES
            );
            javaMailSender.send(mimeMessage);
        }
        catch (Exception e){
            log.error("메일 전송 실패 email={}, err={}", request.email(), e.toString(), e);
            throw new IllegalStateException("메일 전송에 실패했습니다.");
        }
    }

    // 메일 생성
    public MimeMessage createMimeMessage(EmailCodeRequest request, String code) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // setFrom을 지정하지 않으면 내가 yml파일 지정한 이메일이 기본으로 지정됨
        mimeMessageHelper.setTo(request.email());
        mimeMessageHelper.setFrom(username + "@naver.com");
        mimeMessageHelper.setSubject("금품타 인증번호");
        String html = """
            <div style="font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial;">
              <h2>이메일 인증코드</h2>
              <p>아래 인증코드를 입력해 주세요. 유효기간은 5분입니다.</p>
              <div style="font-size:24px; font-weight:700; letter-spacing:2px;">%s</div>
              <p style="color:#888; font-size:12px;">만약 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
            </div>
        """.formatted(code);
        mimeMessageHelper.setText(html, true);
        return mimeMessage;
    }

    // 인증번호 생성
    public String createCode(){
        Random r = new Random();
        StringBuilder randomNumber = new StringBuilder();
        for(int i = 0; i < 6; i++) {
            randomNumber.append(r.nextInt(10));
        }
        return randomNumber.toString();
    }

    // 인증코드 요청 보낸 사람과 현재 api 호출하는 사람의 학교 이메일이 동일한지 검증
    public boolean isEmailOwner(EmailCodeVerifyRequest request, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
        String schoolEmail = user.getSchoolEmail();
        if(schoolEmail == null){
            return false;
        }
        if(user.getSchoolEmail().equals(request.email())){
            return false;
        }
        return true;
    }
}
