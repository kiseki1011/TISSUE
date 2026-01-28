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

// TODO: Add javadoc
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

    /**
     * The email address or Subject ID of the social service provider.
     */
    @Column(nullable = false)
    private String identifier;

    /**
     * The encrypted password. Can be null if using OAuth2.
     */
    @Column(nullable = true)
    @Nullable
    private String credential;

    public static AuthIdentity createEmailIdentity(Member member, String email, String encryptedPassword) {
        AuthIdentity identity = new AuthIdentity();
        identity.member = member;
        identity.provider = AuthProvider.EMAIL;
        identity.identifier = email;
        identity.credential = encryptedPassword;
        return identity;
    }

    public static AuthIdentity createSocialIdentity(Member member, AuthProvider provider, String identifier) {
        AuthIdentity identity = new AuthIdentity();
        identity.member = member;
        identity.provider = provider;
        identity.identifier = identifier;
        identity.credential = null;
        return identity;
    }

    public void updateCredential(String newEncryptedPassword) {
        this.credential = newEncryptedPassword;
    }

    public void updateIdentifier(String newIdentifier) {
        this.identifier = newIdentifier;
    }
}
