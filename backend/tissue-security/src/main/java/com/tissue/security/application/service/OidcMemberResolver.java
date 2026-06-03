package com.tissue.security.application.service;

import static com.tissue.security.domain.exception.AuthenticationErrorCode.OIDC_EMAIL_DOMAIN_NOT_ALLOWED;
import static com.tissue.security.domain.exception.AuthenticationErrorCode.OIDC_EMAIL_MISSING;
import static com.tissue.security.domain.exception.AuthenticationErrorCode.OIDC_PROVISIONING_DISABLED;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.global.setup.GlobalDefaultSetupService;
import com.tissue.security.application.dto.OidcUserInfo;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.config.TissueAuthProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.shared.exception.base.ForbiddenException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps a validated IdP identity ({@code sub}) to a Tissue {@link Member}, provisioning one on first login.
 *
 * <p>This is the bridge between OIDC authentication and Tissue's own user model. The IdP token carries no
 * {@code memberId} or role, so we look the member up by {@code (OIDC, sub)} and, if absent, create it
 * (JIT provisioning). The resulting member then drives Tissue's own token issuance.
 *
 * <p>OIDC always requires a valid email. The {@code tissue.security.email-required} flag is for LOCAL
 * signup only and has no effect here.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OidcMemberResolver {

    private final AuthenticationIdentityRepository identityRepository;
    private final MemberCommandRepository memberCommandRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final GlobalDefaultSetupService globalDefaultSetupService;
    private final TissueAuthProperties authProperties;

    public Member resolve(OidcUserInfo userInfo) {
        Member member = identityRepository
                .findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, userInfo.subject())
                .map(AuthenticationIdentity::getMember)
                .orElseGet(() -> provision(userInfo));
        syncProfile(member, userInfo);
        return member;
    }

    /**
     * Sync email/name with the IdP on each login (the IdP should be the source of truth).
     * Username is intentionally left untouched.
     */
    private void syncProfile(Member member, OidcUserInfo userInfo) {
        String email = userInfo.email();
        if (email != null && !email.equals(member.getEmail())) {
            member.updateEmail(email);
        }
        String name = userInfo.name();
        if (name != null && !name.equals(member.getName())) {
            member.updateName(name);
        }
    }

    private Member provision(OidcUserInfo userInfo) {
        if (!authProperties.getOidc().isAutoProvision()) {
            throw new ForbiddenException(OIDC_PROVISIONING_DISABLED);
        }

        String email = ensureEmailAvailable(userInfo);
        ensureAllowedDomain(email);

        boolean firstUser = memberQueryRepository.count() == 0;

        try {
            Member member = createMember(email, userInfo, firstUser);
            Member saved = memberCommandRepository.save(member);
            identityRepository.save(AuthenticationIdentity.createOidcIdentity(saved, userInfo.subject()));

            if (firstUser) {
                globalDefaultSetupService.setupDefaults();
            }

            return saved;

        } catch (DataIntegrityViolationException e) {
            return identityRepository
                    .findByProviderAndIdentifier(AuthenticationIdentityProvider.OIDC, userInfo.subject())
                    .map(AuthenticationIdentity::getMember)
                    .orElseThrow(() -> e);
        }
    }

    private String ensureEmailAvailable(OidcUserInfo userInfo) {
        String email = userInfo.email();
        if (email == null || email.isBlank()) {
            throw new ForbiddenException(OIDC_EMAIL_MISSING);
        }
        return email;
    }

    private Member createMember(String email, OidcUserInfo userInfo, boolean firstUser) {
        String username = userInfo.username();
        String name = userInfo.name() != null ? userInfo.name() : username;
        return firstUser ? Member.createAsSuperAdmin(email, username, name) : Member.create(email, username, name);
    }

    private void ensureAllowedDomain(String email) {
        var allowedDomains = authProperties.getOidc().getAllowedEmailDomains();
        if (allowedDomains.isEmpty()) {
            return;
        }
        int at = email.lastIndexOf('@');
        String domain = at >= 0 ? email.substring(at + 1).toLowerCase(Locale.ROOT) : "";
        boolean allowed =
                allowedDomains.stream().anyMatch(d -> d.toLowerCase(Locale.ROOT).equals(domain));
        if (!allowed) {
            throw new ForbiddenException(OIDC_EMAIL_DOMAIN_NOT_ALLOWED);
        }
    }
}
