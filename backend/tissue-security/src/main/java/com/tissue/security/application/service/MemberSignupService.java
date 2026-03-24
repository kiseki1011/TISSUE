package com.tissue.security.application.service;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.usecase.MemberSignupUseCase;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.security.domain.exception.MemberSignupConflictException;
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
    private final TokenPairCreateService tokenPairCreateService;
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
        AuthenticationIdentityProvider provider = AuthenticationIdentityProvider.valueOf(providerStr);

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
                    List.of(new SimpleGrantedAuthority(savedMember.getRole().getAuthority()));

            TokenPair tokens = tokenPairCreateService.createTokens(
                    savedMember.getId(), savedMember.getEmail(), savedMember.getUsername(), authorities);

            return OAuthSignupResponse.builder()
                    .accessToken(tokens.accessToken())
                    .refreshToken(tokens.refreshToken())
                    .build();

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(email, cmd.username(), e);
        }
    }
}
