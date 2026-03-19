package com.tissue.security.application.service;

import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_EMAIL;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_USERNAME;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.usecase.MemberAccountUseCase;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationProvider;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailIdentityNotFoundException;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.security.domain.exception.MemberSignupConflictException;
import com.tissue.shared.exception.base.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Override
    public void linkEmailAuthentication(String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        if (authenticationIdentityRepository
                .findByProviderAndIdentifier(AuthenticationProvider.EMAIL, member.getEmail())
                .isPresent()) {
            throw new IllegalArgumentException("Password already exists. Use update password instead.");
        }

        AuthenticationIdentity emailIdentity = AuthenticationIdentity.createEmailIdentity(
                member, member.getEmail(), passwordEncoder.encode(newPassword));

        authenticationIdentityRepository.save(emailIdentity);
    }

    @Override
    public void linkOAuthAccount(String registerToken, Long memberId) {
        TokenClaims claims = tokenProvider.validateRegisterToken(registerToken);

        String providerStr = claims.provider();
        String identifier = claims.identifier();
        String email = claims.email();
        AuthenticationProvider provider = AuthenticationProvider.valueOf(providerStr);

        memberAccountValidator.ensureDomainAllowed(email);

        Member member = memberFinder.getActiveBy(memberId);

        if (authenticationIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .isPresent()) {
            throw new MemberSignupConflictException(
                    email, "OAuth Account already linked", new DataIntegrityViolationException("Duplicate Identity"));
        }

        AuthenticationIdentity socialIdentity =
                AuthenticationIdentity.createSocialIdentity(member, provider, identifier);
        authenticationIdentityRepository.save(socialIdentity);
    }

    @Override
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberAccountValidator.ensureUniqueUsername(newUsername);

        try {
            member.updateUsername(newUsername);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException(DUPLICATE_USERNAME, e);
        }
    }

    @Override
    public void updateEmail(String newEmail, String verificationToken, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String oldEmail = member.getEmail();

        memberAccountValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.isTokenVerified(newEmail, verificationToken)) {
            throw new EmailNotVerifiedException(newEmail);
        }

        try {
            member.updateEmail(newEmail);

            authenticationIdentityRepository
                    .findByProviderAndIdentifier(AuthenticationProvider.EMAIL, oldEmail)
                    .ifPresent(identity -> identity.updateIdentifier(newEmail));

        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException(DUPLICATE_EMAIL, e);
        }
    }

    @Override
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword));

        AuthenticationIdentity authenticationIdentity = authenticationIdentityRepository
                .findByProviderAndIdentifier(AuthenticationProvider.EMAIL, member.getEmail())
                .orElseThrow(() -> new EmailIdentityNotFoundException(memberId, member.getEmail()));

        authenticationIdentity.updateCredential(passwordEncoder.encode(newPassword));
    }

    @Override
    public void withdraw(String password, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(member.getEmail(), password));
        memberAccountValidator.ensureWithdrawable(member);

        member.withdraw();
    }

    @Override
    public void checkEmailAvailability(String email) {
        memberAccountValidator.ensureUniqueEmail(email);
    }

    @Override
    public void checkUsernameAvailability(String username) {
        memberAccountValidator.ensureUniqueUsername(username);
    }
}
