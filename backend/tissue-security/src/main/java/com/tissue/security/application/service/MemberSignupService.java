package com.tissue.security.application.service;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.global.setup.GlobalDefaultSetupService;
import com.tissue.security.application.dto.TokenPair;
import com.tissue.security.application.dto.command.SignupMemberCommand;
import com.tissue.security.application.dto.command.SignupOAuthMemberCommand;
import com.tissue.security.application.dto.response.MemberSignupResponse;
import com.tissue.security.application.dto.response.OAuthSignupResponse;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.usecase.MemberSignupUseCase;
import com.tissue.security.config.TissueSecurityProperties;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.security.domain.TokenClaims;
import com.tissue.security.domain.TokenProvider;
import com.tissue.security.domain.exception.EmailNotVerifiedException;
import com.tissue.security.domain.exception.MemberSignupConflictException;
import java.util.List;
import java.util.Objects;
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
    private final SignupGuardrails signupGuardrails;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final TokenPairCreateService tokenPairCreateService;
    private final MemberEmailVerificationService memberEmailVerificationService;
    private final TissueSecurityProperties tissueSecurityProperties;
    private final GlobalDefaultSetupService globalDefaultSetupService;

    @Override
    public MemberSignupResponse signup(SignupMemberCommand cmd) {
        signupGuardrails.ensureSignupAllowed();
        memberAccountValidator.ensureUniqueUsername(cmd.username());

        if (tissueSecurityProperties.isEmailRequired()) {
            return signupWithEmailVerification(cmd);
        }

        return signupWithUsernameOnly(cmd);
    }

    private MemberSignupResponse signupWithEmailVerification(SignupMemberCommand cmd) {
        String email = Objects.requireNonNull(cmd.email(), "Email is required for email verification signup");
        String verifiedToken = Objects.requireNonNull(cmd.verifiedToken(), "Verified token is required");

        memberAccountValidator.ensureUniqueEmail(email);

        if (!memberEmailVerificationService.isTokenVerified(email, verifiedToken)) {
            throw new EmailNotVerifiedException(email);
        }

        boolean firstUser = signupGuardrails.isFirstUser();
        Member member = firstUser
                ? Member.createAsSuperAdmin(email, cmd.username(), cmd.name())
                : Member.create(email, cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            String encodedPassword = passwordEncoder.encode(cmd.password());

            AuthenticationIdentity emailIdentity =
                    AuthenticationIdentity.createEmailIdentity(savedMember, email, encodedPassword);
            AuthenticationIdentity usernameIdentity =
                    AuthenticationIdentity.createUsernameIdentity(savedMember, cmd.username(), encodedPassword);
            authenticationIdentityRepository.save(emailIdentity);
            authenticationIdentityRepository.save(usernameIdentity);

            if (firstUser) {
                globalDefaultSetupService.setupDefaults();
            }

            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(email, cmd.username(), e);
        }
    }

    private MemberSignupResponse signupWithUsernameOnly(SignupMemberCommand cmd) {
        boolean firstUser = signupGuardrails.isFirstUser();
        Member member = firstUser
                ? Member.createAsSuperAdminWithoutEmail(cmd.username(), cmd.name())
                : Member.createWithoutEmail(cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthenticationIdentity authenticationIdentity = AuthenticationIdentity.createUsernameIdentity(
                    savedMember, cmd.username(), passwordEncoder.encode(cmd.password()));
            authenticationIdentityRepository.save(authenticationIdentity);

            if (firstUser) {
                globalDefaultSetupService.setupDefaults();
            }

            return MemberSignupResponse.from(savedMember);

        } catch (DataIntegrityViolationException e) {
            throw new MemberSignupConflictException(null, cmd.username(), e);
        }
    }

    @Override
    public OAuthSignupResponse signupWithOAuth(SignupOAuthMemberCommand cmd) {
        signupGuardrails.ensureSignupAllowed();

        TokenClaims claims = tokenProvider.validateRegisterToken(cmd.registerToken());

        String providerStr = claims.provider();
        String identifier = claims.identifier();
        String email = claims.email();
        AuthenticationIdentityProvider provider = AuthenticationIdentityProvider.fromRegistrationId(providerStr);

        memberAccountValidator.ensureUniqueUsername(cmd.username());
        memberAccountValidator.ensureUniqueEmail(email);

        boolean firstUser = signupGuardrails.isFirstUser();
        Member member = firstUser
                ? Member.createAsSuperAdmin(email, cmd.username(), cmd.name())
                : Member.create(email, cmd.username(), cmd.name());

        try {
            Member savedMember = memberCommandRepository.save(member);

            AuthenticationIdentity socialIdentity =
                    AuthenticationIdentity.createSocialIdentity(savedMember, provider, identifier);
            authenticationIdentityRepository.save(socialIdentity);

            if (firstUser) {
                globalDefaultSetupService.setupDefaults();
            }

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
