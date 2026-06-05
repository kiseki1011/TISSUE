package com.tissue.security.filter;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.security.domain.PatScope;
import com.tissue.security.domain.PersonalAccessToken;
import com.tissue.shared.auth.MemberDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests carrying a Personal Access Token (PAT) in the {@code Authorization: Bearer} header.
 */
@RequiredArgsConstructor
public class PatAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final PersonalAccessTokenService personalAccessTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawToken = resolveToken(request);
        if (rawToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            personalAccessTokenService.authenticate(rawToken).ifPresent(token -> authenticateAs(token, request));
        }
        filterChain.doFilter(request, response);
    }

    @Nullable
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.startsWith(PersonalAccessTokenService.TOKEN_PREFIX) ? token : null;
    }

    private void authenticateAs(PersonalAccessToken token, HttpServletRequest request) {
        Member member = token.getMember();
        Collection<GrantedAuthority> authorities = buildAuthorities(token.getScope());
        MemberDetails principal =
                new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), authorities);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * A PAT is capped at {@link SystemRole#USER} regardless of the owning member's system role.
     *
     * <p>To put it straight, agents should always be USER-level actors, meaning even a PAT owned by
     * an ADMIN must never act as ADMIN.
     */
    private static Collection<GrantedAuthority> buildAuthorities(PatScope scope) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(SystemRole.USER.getAuthority()));
        authorities.add(new SimpleGrantedAuthority("SCOPE_READ"));
        if (scope.canWrite()) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_WRITE"));
        }
        return authorities;
    }
}
