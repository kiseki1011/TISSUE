package com.tissue.security.application.service;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.adapter.persistence.PersonalAccessTokenRepository;
import com.tissue.security.application.dto.GeneratedToken;
import com.tissue.security.domain.PatScope;
import com.tissue.security.domain.PersonalAccessToken;
import com.tissue.security.util.TokenHashUtil;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalAccessTokenService {

    public static final String TOKEN_PREFIX = "tissue_pat_";
    private static final int RANDOM_BYTE_LENGTH = 32;
    private static final Duration LAST_USED_THROTTLE = Duration.ofHours(1);

    private final PersonalAccessTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public GeneratedToken generate(Member member, String name, PatScope scope, @Nullable Duration ttl) {
        String rawToken = generateRawToken();
        PersonalAccessToken token = PersonalAccessToken.create(member, name, TokenHashUtil.hash(rawToken), scope, ttl);
        return new GeneratedToken(repository.save(token), rawToken);
    }

    @Transactional
    public Optional<PersonalAccessToken> authenticate(String rawToken) {
        Optional<PersonalAccessToken> token = repository
                .findByTokenHash(TokenHashUtil.hash(rawToken))
                .filter(PersonalAccessToken::isUsable)
                .filter(found -> found.getMember().isActive());
        token.ifPresent(found -> found.touchIfStale(LAST_USED_THROTTLE));
        return token;
    }

    @Transactional(readOnly = true)
    public List<PersonalAccessToken> listFor(Long memberId) {
        return repository.findAllByMember_Id(memberId);
    }

    @Transactional
    public void revoke(Long memberId, Long tokenId) {
        repository.findByIdAndMember_Id(tokenId, memberId).ifPresent(PersonalAccessToken::revoke);
    }

    @Transactional
    public void revokeAllFor(Long memberId) {
        repository.findAllByMember_Id(memberId).forEach(PersonalAccessToken::revoke);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RANDOM_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
