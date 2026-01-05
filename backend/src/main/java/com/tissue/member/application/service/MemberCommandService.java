package com.tissue.member.application.service;

import com.tissue.member.application.dto.request.SignupMemberCommand;
import com.tissue.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.member.application.dto.response.MemberSignupResponse;
import com.tissue.member.application.port.in.MemberCommandUseCase;
import com.tissue.member.application.port.out.AuthIdentityRepository;
import com.tissue.member.application.port.out.MemberCommandRepository;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.application.service.validator.MemberValidator;
import com.tissue.member.domain.AuthIdentity;
import com.tissue.member.domain.AuthProvider;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.creator.AuthIdentityManager;
import com.tissue.member.domain.exception.MemberExceptions;
import com.tissue.security.authentication.application.port.out.RefreshTokenRepository;
import com.tissue.security.authentication.exception.AuthenticationExceptions;
import com.tissue.security.authentication.jwt.JwtTokenService;
import com.tissue.security.authentication.presentation.dto.response.OAuthSignupResponse;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 관련 변경 작업(가입, 수정, 탈퇴)을 처리하는 서비스입니다.
 *
 * <p>
 * <b>주요 변경 사항 (AuthIdentity 도입):</b><br>
 * 비밀번호는 이제 `Member` 엔티티가 아닌 `AuthIdentity` 엔티티에서 관리됩니다.
 * 따라서 회원가입 시 `Member`와 `AuthIdentity`를 함께 생성하며, 비밀번호 변경 시 `AuthIdentity`를 수정합니다.
 * </p>
 */
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
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public MemberSignupResponse signup(SignupMemberCommand cmd) {
        memberValidator.ensureUniqueEmail(cmd.email());
        memberValidator.ensureUniqueUsername(cmd.username());

        if (!memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.verificationToken())) {
            throw AuthenticationExceptions.invalidVerificationToken();
        }

        Member member = Member.create(cmd.email(), cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthIdentity authIdentity =
                    authIdentityManager.create(savedMember, cmd.provider(), cmd.email(), cmd.password());
            authIdentityRepository.save(authIdentity);

            memberEmailVerificationService.clearVerification(cmd.email());
            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.signUpConflict(cmd.email(), cmd.username(), e);
        }
    }

    @Override
    public OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand cmd) {
        Claims claims = jwtTokenService.validateRegisterToken(cmd.registerToken());

        String providerStr = claims.get(JwtTokenService.CLAIM_PROVIDER, String.class);
        String identifier = claims.get(JwtTokenService.CLAIM_IDENTIFIER, String.class);
        String email = claims.get(JwtTokenService.CLAIM_EMAIL, String.class);
        AuthProvider provider = AuthProvider.valueOf(providerStr);

        memberValidator.ensureUniqueUsername(cmd.username());
        memberValidator.ensureUniqueEmail(email);

        Member member = Member.create(email, cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            // OAuth credential is null
            AuthIdentity authIdentity = authIdentityManager.create(savedMember, provider, identifier, null);
            authIdentityRepository.save(authIdentity);

            // Auto-login after signup
            String accessToken = jwtTokenService.createAccessToken(savedMember.getId(), savedMember.getEmail());
            String refreshToken = jwtTokenService.createRefreshToken(savedMember.getId(), savedMember.getEmail());

            refreshTokenRepository.save(
                    savedMember.getEmail(),
                    refreshToken,
                    Duration.ofSeconds(jwtTokenService.getRefreshTokenValidityInSeconds()));

            return OAuthSignupResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.signUpConflict(email, cmd.username(), e);
        }
    }

    @Override
    public void linkOAuthAccount(String registerToken, Long memberId) {
        Claims claims = jwtTokenService.validateRegisterToken(registerToken);

        String providerStr = claims.get(JwtTokenService.CLAIM_PROVIDER, String.class);
        String identifier = claims.get(JwtTokenService.CLAIM_IDENTIFIER, String.class);
        AuthProvider provider = AuthProvider.valueOf(providerStr);

        Member member = memberFinder.getActiveBy(memberId);

        // Check if this OAuth account is already linked (should be empty if we are here via RegisterToken,
        // but double check)
        if (authIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .isPresent()) {
            throw MemberExceptions.signUpConflict(
                    claims.get(JwtTokenService.CLAIM_EMAIL, String.class),
                    "OAuth Account already linked",
                    new DataIntegrityViolationException("Duplicate Identity"));
        }

        AuthIdentity authIdentity = authIdentityManager.create(member, provider, identifier, null);
        authIdentityRepository.save(authIdentity);
    }

    @Override
    public void updateName(String name, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        member.updateName(name);
    }

    @Override
    public void updateEmail(String newEmail, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);
        String oldEmail = member.getEmail();

        memberValidator.ensureUniqueEmail(newEmail);

        if (!memberEmailVerificationService.isEmailVerified(newEmail)) {
            throw MemberExceptions.emailNotVerified(newEmail);
        }

        try {
            member.updateEmail(newEmail);

            authIdentityRepository
                    .findByProviderAndIdentifier(AuthProvider.EMAIL, oldEmail)
                    .ifPresent(identity -> identity.updateIdentifier(newEmail));

            memberEmailVerificationService.clearVerification(newEmail);
        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.duplicateEmail(newEmail, e);
        }
    }

    @Override
    public void updateUsername(String newUsername, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        memberValidator.ensureUniqueUsername(newUsername);

        try {
            member.updateUsername(newUsername);
        } catch (DataIntegrityViolationException e) {
            throw MemberExceptions.duplicateUsername(newUsername, e);
        }
    }

    @Override
    public void updatePassword(String originalPassword, String newPassword, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(member.getEmail(), originalPassword));

        AuthIdentity authIdentity = authIdentityRepository
                .findByProviderAndIdentifier(AuthProvider.EMAIL, member.getEmail())
                .orElseThrow(() -> MemberExceptions.notFound(memberId)); // TODO: IdentityNotFound

        authIdentity.updateCredential(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void withdraw(String password, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(member.getEmail(), password));

        memberValidator.ensureWithdrawable(member);

        member.withdraw();
    }
}
