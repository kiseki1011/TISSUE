package com.tissue.application.service;

import com.tissue.application.dto.response.OAuthSignupResponse;
import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.application.port.usecase.MemberSignupUseCase;
import com.tissue.domain.TokenClaims;
import com.tissue.domain.TokenProvider;
import com.tissue.domain.exception.EmailNotVerifiedException;
import com.tissue.feature.member.application.dto.request.SignupMemberCommand;
import com.tissue.feature.member.application.dto.request.SignupOAuthMemberCommand;
import com.tissue.feature.member.application.dto.response.MemberSignupResponse;
import com.tissue.feature.member.application.port.out.AuthIdentityRepository;
import com.tissue.feature.member.application.port.out.MemberCommandRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.MemberValidator;
import com.tissue.feature.member.domain.AuthIdentity;
import com.tissue.feature.member.domain.AuthProvider;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.creator.AuthIdentityManager;
import com.tissue.feature.member.domain.exception.MemberSignupConflictException;
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

    private final MemberFinder memberFinder;
    private final MemberCommandRepository memberCommandRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityManager authIdentityManager;
    private final MemberValidator memberValidator;
    private final PasswordEncoder passwordEncoder;
    private final MemberEmailVerificationService memberEmailVerificationService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // TODO: 현재 AuthIdentityCreator 구현체 내에서 passwordEncoder를 사용해서 암호화를 하고 있음.
    //  그냥 여기서 진행하는게 좋지 않을까?
    @Override
    public MemberSignupResponse signup(SignupMemberCommand cmd) {
        memberValidator.ensureSignupAllowed();
        memberValidator.ensureDomainAllowedIfPrivate(cmd.email());
        memberValidator.ensureUniqueEmail(cmd.email());
        memberValidator.ensureUniqueUsername(cmd.username());

        if (!memberEmailVerificationService.validateSignupToken(cmd.email(), cmd.signupToken())) {
            throw new EmailNotVerifiedException(cmd.email());
        }

        Member member = Member.create(cmd.email(), cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthIdentity authIdentity = authIdentityManager.create(
                    savedMember, cmd.provider(), cmd.email(), passwordEncoder.encode(cmd.password()));
            authIdentityRepository.save(authIdentity);

            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(cmd.email(), cmd.username(), e);
        }
    }

    @Override
    public OAuthSignupResponse signupOAuth(SignupOAuthMemberCommand cmd) {
        TokenClaims claims = tokenProvider.validateRegisterToken(cmd.registerToken());

        String providerStr = claims.provider();
        String identifier = claims.identifier();
        String email = claims.email();
        AuthProvider provider = AuthProvider.valueOf(providerStr);

        memberValidator.ensureDomainAllowedIfPrivate(email);
        memberValidator.ensureUniqueUsername(cmd.username());
        memberValidator.ensureUniqueEmail(email);

        Member member = Member.create(email, cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthIdentity authIdentity = authIdentityManager.create(savedMember, provider, identifier, null);
            authIdentityRepository.save(authIdentity);

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

    @Override
    public void linkOAuthAccount(String registerToken, Long memberId) {
        TokenClaims claims = tokenProvider.validateRegisterToken(registerToken);

        String providerStr = claims.provider();
        String identifier = claims.identifier();
        String email = claims.email();
        AuthProvider provider = AuthProvider.valueOf(providerStr);

        memberValidator.ensureDomainAllowedIfPrivate(email);

        Member member = memberFinder.getActiveBy(memberId);

        if (authIdentityRepository
                .findByProviderAndIdentifier(provider, identifier)
                .isPresent()) {
            throw new MemberSignupConflictException(
                    email, "OAuth Account already linked", new DataIntegrityViolationException("Duplicate Identity"));
        }

        AuthIdentity authIdentity = authIdentityManager.create(member, provider, identifier, null);
        authIdentityRepository.save(authIdentity);
    }
}
