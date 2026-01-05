package com.tissue.security.authentication.infrastructure.context;

import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import com.tissue.security.authentication.domain.MemberUserDetails;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentMemberProvider implements CurrentMemberProvider {

    @Override
    public Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberUserDetails)) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        return ((MemberUserDetails) authentication.getPrincipal()).getMemberId();
    }
}
