package com.tissue.member.domain;

import com.tissue.common.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "auth_identity",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_auth_identity_provider_identifier",
                    columnNames = {"provider", "identifier"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthIdentity extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /** 이메일 주소 또는 소셜 서비스의 식별자(Subject ID) */
    @Column(nullable = false)
    private String identifier;

    /** 암호화된 비밀번호 (OAuth 사용 시 null 가능) */
    @Column(nullable = true)
    @Nullable
    private String credential;

    /**
     * 이메일 기반 인증 수단을 생성
     *
     * @param member 연결될 회원 엔티티
     * @param email 로그인 아이디로 사용할 이메일
     * @param encryptedPassword 암호화된 비밀번호
     * @return 생성된 AuthIdentity 객체
     */
    public static AuthIdentity createEmailIdentity(Member member, String email, String encryptedPassword) {
        AuthIdentity identity = new AuthIdentity();
        identity.member = member;
        identity.provider = AuthProvider.EMAIL;
        identity.identifier = email;
        identity.credential = encryptedPassword;
        return identity;
    }

    public void updateCredential(String newEncryptedPassword) {
        this.credential = newEncryptedPassword;
    }

    /**
     * 식별자(이메일 등)를 변경
     * Member의 이메일이 변경될 때 동기화를 위해 사용
     *
     * @param newIdentifier 새로운 식별자
     */
    public void updateIdentifier(String newIdentifier) {
        this.identifier = newIdentifier;
    }
}
