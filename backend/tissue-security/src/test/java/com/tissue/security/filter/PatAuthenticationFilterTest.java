package com.tissue.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.security.domain.PatScope;
import com.tissue.security.domain.PersonalAccessToken;
import com.tissue.shared.auth.MemberDetails;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PatAuthenticationFilterTest {

    @Mock
    PersonalAccessTokenService personalAccessTokenService;

    PatAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PatAuthenticationFilter(personalAccessTokenService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: authenticates as the owning member when the PAT is valid")
    void authenticatesAsOwningMemberWhenPatIsValid() throws Exception {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(7L);
        when(member.getEmail()).thenReturn("backend-bot@tissue.com");
        when(member.getUsername()).thenReturn("backend-bot");

        PersonalAccessToken token = mock(PersonalAccessToken.class);
        when(token.getMember()).thenReturn(member);
        when(token.getScope()).thenReturn(PatScope.READ_WRITE);
        when(personalAccessTokenService.authenticate("tissue_pat_valid")).thenReturn(Optional.of(token));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tissue_pat_valid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        MemberDetails principal = (MemberDetails) authentication.getPrincipal();
        assertThat(principal.getMemberId()).isEqualTo(7L);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER", "SCOPE_READ", "SCOPE_WRITE")
                .doesNotContain("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("success: grants only read scope for a read-only token")
    void grantsOnlyReadScopeForReadOnlyToken() throws Exception {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(9L);
        when(member.getEmail()).thenReturn("robot@tissue.com");
        when(member.getUsername()).thenReturn("robot");

        PersonalAccessToken token = mock(PersonalAccessToken.class);
        when(token.getMember()).thenReturn(member);
        when(token.getScope()).thenReturn(PatScope.READ_ONLY);
        when(personalAccessTokenService.authenticate("tissue_pat_robot")).thenReturn(Optional.of(token));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tissue_pat_robot");

        // when
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities()).extracting("authority").contains("SCOPE_READ");
        assertThat(authentication.getAuthorities()).extracting("authority").doesNotContain("SCOPE_WRITE");
    }

    @Test
    @DisplayName("fail: leaves the context unauthenticated when no Authorization header is present")
    void leavesContextUnauthenticatedWhenNoHeader() throws Exception {
        // when
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(personalAccessTokenService);
    }

    @Test
    @DisplayName("fail: ignores a bearer token that is not a PAT")
    void ignoresBearerTokenThatIsNotAPat() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig");

        // when
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(personalAccessTokenService);
    }

    @Test
    @DisplayName("fail: leaves context unauthenticated when the PAT is invalid")
    void leavesContextUnauthenticatedWhenPatIsInvalid() throws Exception {
        // given
        when(personalAccessTokenService.authenticate("tissue_pat_bad")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tissue_pat_bad");
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
