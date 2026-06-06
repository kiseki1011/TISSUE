package com.tissue.security.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.port.usecase.MemberAccountUseCase;
import com.tissue.security.config.TissueAuthProperties;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.security.domain.exception.EmailIdentityNotFoundException;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.exception.base.UnauthorizedException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberAccountValidator memberAccountValidator;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;
    private final TissueSecurityProperties tissueSecurityProperties;
    private final TissueAuthProperties tissueAuthProperties;
    private final OwnedAgentDeactivationService ownedAgentDeactivationService;

    @Override
    public void linkEmailAuthentication(String newPassword, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
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
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);

        memberAccountValidator.ensureUniqueUsername(newUsername);

        member.updateUsername(newUsername);
        authenticationIdentityRepository
                .findByMemberIdAndProvider(memberId, AuthenticationIdentityProvider.USERNAME)
                .ifPresent(identity -> identity.updateIdentifier(newUsername));
    }

    @Override
    public void updateEmail(String newEmail, String verificationToken, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
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
        Member member = memberFinder.getActiveById(memberId);

        String loginIdentifier = getLoginIdentifier(member);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginIdentifier, originalPassword));

        List<AuthenticationIdentity> identities = authenticationIdentityRepository.findAllByMemberIdAndProviderIn(
                memberId, List.of(AuthenticationIdentityProvider.EMAIL, AuthenticationIdentityProvider.USERNAME));

        if (identities.isEmpty()) {
            throw new EmailIdentityNotFoundException(memberId);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        identities.forEach(identity -> identity.updateCredential(encodedPassword));

        refreshTokenRepository.deleteByMemberId(memberId);
    }

    @Override
    public void withdraw(@Nullable String password, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);

        // LOCAL mode re-authenticates with the current password. OIDC has no local password, so the
        // authenticated session itself is sufficient confirmation for withdrawal.
        if (tissueAuthProperties.getMode() == TissueAuthProperties.Mode.LOCAL) {
            String loginIdentifier = getLoginIdentifier(member);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginIdentifier, password));
        }
        memberAccountValidator.ensureWithdrawable(member);

        member.withdraw();
        refreshTokenRepository.deleteByMemberId(memberId);
        ownedAgentDeactivationService.deactivateAgentsOf(memberId);
    }

    @Override
    public void restore(String identifier, String password) {
        AuthenticationIdentityProvider provider = tissueSecurityProperties.isEmailRequired()
                ? AuthenticationIdentityProvider.EMAIL
                : AuthenticationIdentityProvider.USERNAME;

        AuthenticationIdentity identity = authenticationIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .orElseThrow(() -> new UnauthorizedException(AuthenticationErrorCode.RESTORE_INVALID_CREDENTIALS));

        String credential = identity.getCredential();
        if (credential == null || !passwordEncoder.matches(password, credential)) {
            throw new UnauthorizedException(AuthenticationErrorCode.RESTORE_INVALID_CREDENTIALS);
        }

        Member member = identity.getMember();
        if (!member.isDeleted()) {
            throw new ResourceConflictException(AuthenticationErrorCode.RESTORE_NOT_DELETED);
        }

        member.restore();
    }

    private String getLoginIdentifier(Member member) {
        if (tissueSecurityProperties.isEmailRequired()) {
            return Objects.requireNonNull(member.getEmail(), "Email is required for login");
        }
        return member.getUsername();
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
