package com.tissue.member.application.service;

import com.tissue.authentication.application.dto.response.OAuthSignupResponse;
import com.tissue.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.authentication.application.port.out.TokenProvider;
import com.tissue.common.enums.SupportedLanguage;
import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.creator.AuthIdentityManager;
import com.tissue.member.domain.exception.DuplicateEmailException;
import com.tissue.member.domain.exception.DuplicateUsernameException;
import com.tissue.member.domain.exception.EmailNotVerifiedException;
import com.tissue.member.domain.exception.MemberNotFoundException;
import com.tissue.member.domain.exception.MemberSignupConflictException;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberCommandService implements MemberCommandUseCase {

    private final MemberFinder memberFinder;
    private final MemberCommandRepository memberCommandRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityManager authIdentityManager;
    private final MemberValidator memberValidator;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // TODO: Consider signup -> signupWithEmail
    // TODO: Hardcode the provider as EMAIL in this method
    @Override
    public MemberSignupResponse signup(SignupMemberCommand cmd) {
        memberValidator.ensureSignupAllowed();
        memberValidator.ensureDomainAllowedIfPrivate(cmd.email());
        memberValidator.ensureUniqueEmail(cmd.email());
        memberValidator.ensureUniqueUsername(cmd.username());

        // validate secure signup token
        if (!memberEmailVerificationService.validateSignupToken(cmd.email(), cmd.signupToken())) {
            throw new EmailNotVerifiedException(cmd.email());
        }

        Member member = Member.create(cmd.email(), cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthIdentity authIdentity =
                    authIdentityManager.create(savedMember, cmd.provider(), cmd.email(), cmd.password());
            authIdentityRepository.save(authIdentity);

            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(cmd.email(), cmd.username(), e);
        }
    }

    @Override
    public OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand cmd) {
        Claims claims = tokenProvider.validateRegisterToken(cmd.registerToken());

        String providerStr = claims.get(TokenProvider.CLAIM_PROVIDER, String.class);
        String identifier = claims.get(TokenProvider.CLAIM_IDENTIFIER, String.class);
        String email = claims.get(TokenProvider.CLAIM_EMAIL, String.class);
        AuthProvider provider = AuthProvider.valueOf(providerStr);

        memberValidator.ensureDomainAllowedIfPrivate(email);
        memberValidator.ensureUniqueUsername(cmd.username());
        memberValidator.ensureUniqueEmail(email);

        Member member = Member.create(email, cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthIdentity authIdentity = authIdentityManager.create(savedMember, provider, identifier, null);
            authIdentityRepository.save(authIdentity);

            // auto-login after signup
            var authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    savedMember.getRole().name()));
            String accessToken =
                    tokenProvider.createAccessToken(savedMember.getId(), savedMember.getEmail(), authorities);
            String refreshToken =
                    tokenProvider.createRefreshToken(savedMember.getId(), savedMember.getEmail(), authorities);

            refreshTokenRepository.save(
                    savedMember.getEmail(),
                    refreshToken,
                    Duration.ofSeconds(tokenProvider.getRefreshTokenValidityInSeconds()));

            return OAuthSignupResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(email, cmd.username(), e);
        }
    }

    @Override
    public void linkOAuthAccount(String registerToken, Long memberId) {
        Claims claims = tokenProvider.validateRegisterToken(registerToken);

        String providerStr = claims.get(TokenProvider.CLAIM_PROVIDER, String.class);
        String identifier = claims.get(TokenProvider.CLAIM_IDENTIFIER, String.class);
        String email = claims.get(TokenProvider.CLAIM_EMAIL, String.class);
        AuthProvider provider = AuthProvider.valueOf(providerStr);

        memberValidator.ensureDomainAllowedIfPrivate(email);

        Member member = memberFinder.getActiveBy(memberId);

        if (authIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .isPresent()) {
            throw new MemberSignupConflictException(
                    claims.get(TokenProvider.CLAIM_EMAIL, String.class),
                    "OAuth Account already linked",
                    new DataIntegrityViolationException("Duplicate Identity"));
        }

        AuthIdentity authIdentity = authIdentityManager.create(member, provider, identifier, null);
        authIdentityRepository.save(authIdentity);
    }

    // TODO: Consider changing method name.
    @Override
    public void addPassword(String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        if (authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, member.getEmail())
                .isPresent()) {
            throw new IllegalArgumentException("Password already exists. Use update password instead.");
        }

        AuthIdentity emailIdentity =
                AuthIdentity.createEmailIdentity(member, member.getEmail(), passwordEncoder.encode(newPassword));

        authIdentityRepository.save(emailIdentity);
    }

    @Override
    public void updateName(String name, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateName(name);
    }

    @Override
    public void updateLanguage(SupportedLanguage language, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateLanguage(language);
    }

    @Override
    public void updateEmail(String newEmail, String verificationToken, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String oldEmail = member.getEmail();

        memberValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.validateSignupToken(newEmail, verificationToken)) {
            throw new EmailNotVerifiedException(newEmail);
        }

        try {
            member.updateEmail(newEmail);

            authIdentityRepository
                    .findByProviderAndIdentifier(AuthProvider.EMAIL, oldEmail)
                    .ifPresent(identity -> identity.updateIdentifier(newEmail));

            // Token is already consumed/deleted by validateSignupToken
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(newEmail, e);
        }
    }

    @Override
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberValidator.ensureUniqueUsername(newUsername);

        try {
            member.updateUsername(newUsername);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUsernameException(newUsername, e);
        }
    }

    @Override
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword));

        AuthIdentity authIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, member.getEmail())
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        authIdentity.updateCredential(passwordEncoder.encode(newPassword));
    }

    @Override
    public void withdraw(String password, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(member.getEmail(), password));

        memberValidator.ensureWithdrawable(member);

        member.withdraw();

        // TODO: workspaceMemberRepository.deleteAllByMemberId() + projectMemberRepository.deleteAllByMemberId()
        //  or should i call WorkspaceParticipationService.leave()?
        //  But, then i would need a way to pass the WorkspaceMemberContext. Now to think of it,
        //  passing WorkspaceMemberContext is becoming a hindrance more than helping.
        //        workspaceMemberQueryRepository.softDeleteAllByMemberId(memberId);
        //        projectMemberQueryRepository.softDeleteAllByMemberId(memberId);
    }
}
