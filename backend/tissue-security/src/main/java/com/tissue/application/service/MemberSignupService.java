package com.tissue.application.service;

import com.tissue.application.dto.command.SignupMemberCommand;
import com.tissue.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.application.dto.response.MemberSignupResponse;
import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.application.port.usecase.MemberSignupUseCase;
import com.tissue.domain.AuthenticationIdentity;
import com.tissue.domain.AuthenticationProvider;
import com.tissue.domain.TokenClaims;
import com.tissue.domain.TokenProvider;
import com.tissue.domain.exception.EmailNotVerifiedException;
import com.tissue.domain.exception.MemberSignupConflictException;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberSignupService implements MemberSignupUseCase {

    private final MemberCommandRepository memberCommandRepository;
    private final AuthenticationIdentityRepository authenticationIdentityRepository;
    private final MemberAccountValidator memberAccountValidator;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberEmailVerificationService memberEmailVerificationService;

    @Override
    public MemberSignupResponse signupWithEmail(SignupMemberCommand cmd) {
        memberAccountValidator.ensureSignupAllowed();
        memberAccountValidator.ensureDomainAllowed(cmd.email());
        memberAccountValidator.ensureUniqueEmail(cmd.email());
        memberAccountValidator.ensureUniqueUsername(cmd.username());

        if (!memberEmailVerificationService.isTokenVerified(cmd.email(), cmd.signupToken())) {
            throw new EmailNotVerifiedException(cmd.email());
        }

        Member member = Member.create(cmd.email(), cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthenticationIdentity authenticationIdentity = AuthenticationIdentity.createEmailIdentity(
                    savedMember, cmd.email(), passwordEncoder.encode(cmd.password()));
            authenticationIdentityRepository.save(authenticationIdentity);

            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(cmd.email(), cmd.username(), e);
        }
    }

    @Override
    public OAuthSignupResponse signupWithOAuth(SignupOAuthMemberCommand cmd) {
        TokenClaims claims = tokenProvider.validateRegisterToken(cmd.registerToken());

        String providerStr = claims.provider();
        String identifier = claims.identifier();
        String email = claims.email();
        AuthenticationProvider provider = AuthenticationProvider.valueOf(providerStr);

        memberAccountValidator.ensureDomainAllowed(email);
        memberAccountValidator.ensureUniqueUsername(cmd.username());
        memberAccountValidator.ensureUniqueEmail(email);

        Member member = Member.create(email, cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthenticationIdentity socialIdentity =
                    AuthenticationIdentity.createSocialIdentity(savedMember, provider, identifier);
            authenticationIdentityRepository.save(socialIdentity);

            var authorities =
                    List.of(new SimpleGrantedAuthority(savedMember.getRole().toString()));

            String accessToken = tokenProvider.createAccessToken(
                    savedMember.getId(), savedMember.getEmail(), savedMember.getName(), authorities);
            String refreshToken = tokenProvider.createRefreshToken(
                    savedMember.getId(), savedMember.getEmail(), savedMember.getName(), authorities);

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
}
