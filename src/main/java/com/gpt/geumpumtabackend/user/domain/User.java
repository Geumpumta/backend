package com.gpt.geumpumtabackend.user.domain;


import com.gpt.geumpumtabackend.global.base.BaseEntity;
import com.gpt.geumpumtabackend.global.oauth.user.OAuth2Provider;
import com.gpt.geumpumtabackend.user.dto.request.CompleteRegistrationRequest;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String schoolEmail;

    @Enumerated(value = EnumType.STRING)
    @Getter
    private UserRole role;

    private String name;

    @Column(unique = true)
    private String nickname;

    private String picture;

    @Enumerated(value = EnumType.STRING)
    private OAuth2Provider provider;

    private String providerId;

    @Column(unique = true)
    private String studentId;

    @Enumerated(value = EnumType.STRING)
    private Department department;

    @Builder
    public User(String email, UserRole role, String name, String picture, OAuth2Provider provider, String providerId, Department department) {
        this.email = email;
        this.role = role;
        this.name = name;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
        this.department = department;
    }

    public void completeRegistration(CompleteRegistrationRequest request) {
        this.schoolEmail = request.email();
        this.studentId = request.studentId();
        this.department = Department.from(request.department());
        this.role = UserRole.USER;
    }

    public void setInitialNickname(String nickname) {
        this.nickname = nickname;
    }

}
