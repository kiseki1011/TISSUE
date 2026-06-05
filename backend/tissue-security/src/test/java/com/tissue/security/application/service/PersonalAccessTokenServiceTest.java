package com.tissue.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.adapter.persistence.PersonalAccessTokenRepository;
import com.tissue.security.application.dto.GeneratedToken;
import com.tissue.security.domain.PatScope;
import com.tissue.security.domain.PersonalAccessToken;
import com.tissue.security.util.TokenHashUtil;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalAccessTokenServiceTest {

    @Mock
    PersonalAccessTokenRepository repository;

    @InjectMocks
    PersonalAccessTokenService service;

    @Test
    void generateProducesPrefixedRawTokenAndPersistsItsHash() {
        // given
        Member member = mock(Member.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GeneratedToken generated = service.generate(member, "ci", PatScope.READ_WRITE, Duration.ofDays(30));

        // then
        assertThat(generated.rawToken()).startsWith("tissue_pat_");
        assertThat(generated.token().getTokenHash()).isEqualTo(TokenHashUtil.hash(generated.rawToken()));
        assertThat(generated.token().getScope()).isEqualTo(PatScope.READ_WRITE);
        assertThat(generated.token().isUsable()).isTrue();
    }

    @Test
    void authenticateReturnsTokenWhenUsable() {
        // given
        String raw = "tissue_pat_usable";
        PersonalAccessToken token = newToken(raw, null);
        when(token.getMember().isActive()).thenReturn(true);
        when(repository.findByTokenHash(TokenHashUtil.hash(raw))).thenReturn(Optional.of(token));

        // when & then
        assertThat(service.authenticate(raw)).contains(token);
    }

    @Test
    void authenticateRejectsRevokedToken() {
        // given
        String raw = "tissue_pat_revoked";
        PersonalAccessToken token = newToken(raw, null);
        token.revoke();
        when(repository.findByTokenHash(TokenHashUtil.hash(raw))).thenReturn(Optional.of(token));

        // when & then
        assertThat(service.authenticate(raw)).isEmpty();
    }

    @Test
    void authenticateRejectsExpiredToken() {
        // given
        String raw = "tissue_pat_expired";
        PersonalAccessToken token = newToken(raw, Duration.ofSeconds(-10));
        when(repository.findByTokenHash(TokenHashUtil.hash(raw))).thenReturn(Optional.of(token));

        // when & then
        assertThat(service.authenticate(raw)).isEmpty();
    }

    @Test
    void authenticateRejectsUnknownToken() {
        // given
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        // when & then
        assertThat(service.authenticate("tissue_pat_unknown")).isEmpty();
    }

    private static PersonalAccessToken newToken(String raw, Duration ttl) {
        return PersonalAccessToken.create(mock(Member.class), "ci", TokenHashUtil.hash(raw), PatScope.READ_ONLY, ttl);
    }
}
