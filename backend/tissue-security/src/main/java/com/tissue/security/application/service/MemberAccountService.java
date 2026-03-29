package com.tissue.security.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.usecase.MemberAccountUseCase;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.security.domain.exception.EmailIdentityNotFoundException;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberAccountService implements MemberAccountUseCase {

    private final MemberFinder memberFinder;
    private final AuthenticationIdentityRepository authenticationIdentityRepository;
    private final MemberAccountValidator memberAccountValidator;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;
    private final TokenProvider tokenProvider;
    private final TissueSecurityProperties tissueSecurityProperties;

    @Override
    public void linkEmailAuthentication(String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String email = Objects.requireNonNull(member.getEmail(), "Email is required to link email authentication");

        if (authenticationIdentityRepository
                .findByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, email)
                .isPresent()) {
            throw new ResourceConflictException(AuthenticationErrorCode.EMAIL_IDENTITY_ALREADY_EXISTS);
        }

        AuthenticationIdentity emailIdentity =
                AuthenticationIdentity.createEmailIdentity(member, email, passwordEncoder.encode(newPassword));

        authenticationIdentityRepository.save(emailIdentity);
    }

    @Override
    public void linkOAuthAccount(String registerToken, Long memberId) {
        TokenClaims claims = tokenProvider.validateRegisterToken(registerToken);

        String providerStr = claims.provider();
        String identifier = claims.identifier();
        String email = claims.email();
        AuthenticationIdentityProvider provider = AuthenticationIdentityProvider.valueOf(providerStr);

        memberAccountValidator.ensureDomainAllowed(email);

        Member member = memberFinder.getActiveBy(memberId);

        if (authenticationIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .isPresent()) {
            throw new ResourceConflictException(AuthenticationErrorCode.OAUTH_IDENTITY_ALREADY_LINKED);
        }

        AuthenticationIdentity socialIdentity =
                AuthenticationIdentity.createSocialIdentity(member, provider, identifier);
        authenticationIdentityRepository.save(socialIdentity);
    }

    @Override
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberAccountValidator.ensureUniqueUsername(newUsername);

        member.updateUsername(newUsername);
    }

    @Override
    public void updateEmail(String newEmail, String verificationToken, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String oldEmail = Objects.requireNonNull(member.getEmail(), "Current email is required to update email");

        memberAccountValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.isTokenVerified(newEmail, verificationToken)) {
            throw new EmailNotVerifiedException(newEmail);
        }

        member.updateEmail(newEmail);

        authenticationIdentityRepository
                .findByProviderAndIdentifier(AuthenticationIdentityProvider.EMAIL, oldEmail)
                .ifPresent(identity -> identity.updateIdentifier(newEmail));
    }

    @Override
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        String loginIdentifier = getLoginIdentifier(member);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginIdentifier, originalPassword));

        AuthenticationIdentity authenticationIdentity = authenticationIdentityRepository
                .findByMemberIdAndProvider(memberId, getPasswordProvider())
                .orElseThrow(() -> new EmailIdentityNotFoundException(memberId));

        authenticationIdentity.updateCredential(passwordEncoder.encode(newPassword));
    }

    @Override
    public void withdraw(String password, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        String loginIdentifier = getLoginIdentifier(member);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginIdentifier, password));
        memberAccountValidator.ensureWithdrawable(member);

        member.withdraw();
    }

    private String getLoginIdentifier(Member member) {
        if (tissueSecurityProperties.isEmailRequired()) {
            return Objects.requireNonNull(member.getEmail(), "Email is required for login");
        }
        return member.getUsername();
    }

    private AuthenticationIdentityProvider getPasswordProvider() {
        return tissueSecurityProperties.isEmailRequired()
                ? AuthenticationIdentityProvider.EMAIL
                : AuthenticationIdentityProvider.USERNAME;
    }

    @Override
    @Transactional(readOnly = true)
    public void checkEmailAvailability(String email) {
        memberAccountValidator.ensureUniqueEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkUsernameAvailability(String username) {
        memberAccountValidator.ensureUniqueUsername(username);
    }
}
