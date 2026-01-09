package com.tissue.security.authentication.infrastructure.context;

import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import com.tissue.security.authentication.domain.MemberDetails;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentMemberProvider implements CurrentMemberProvider {

    @Override
    public Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberDetails)) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        return ((MemberDetails) authentication.getPrincipal()).getMemberId();
    }
}
