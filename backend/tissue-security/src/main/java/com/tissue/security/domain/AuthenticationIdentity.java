package com.tissue.security.domain;

import com.tissue.feature.member.domain.Member;
import com.tissue.shared.entity.BaseDateEntity;
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
import lombok.Getter;
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
public class AuthenticationIdentity extends BaseDateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthenticationProvider provider;

    /**
     * The email address or Subject ID of the social service provider.
     */
    @Column(nullable = false)
    private String identifier;

    /**
     * The encrypted password. Can be null if using OAuth2.
     */
    @Nullable
    private String credential;

    @SuppressWarnings("NullAway.Init")
    protected AuthenticationIdentity() {}

    public static AuthenticationIdentity createEmailIdentity(Member member, String email, String encryptedPassword) {
        AuthenticationIdentity identity = new AuthenticationIdentity();
        identity.member = member;
        identity.provider = AuthenticationProvider.EMAIL;
        identity.identifier = email;
        identity.credential = encryptedPassword;
        return identity;
    }

    public static AuthenticationIdentity createSocialIdentity(
            Member member, AuthenticationProvider provider, String identifier) {
        AuthenticationIdentity identity = new AuthenticationIdentity();
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
