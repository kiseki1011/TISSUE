package com.tissue.security.audit;

import com.tissue.shared.auth.MemberDetails;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("memberAuditorProvider")
@RequiredArgsConstructor
public class MemberAuditorProvider implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof MemberDetails userDetails) {
            return Optional.ofNullable(userDetails.getMemberId());
        }

        return Optional.empty();
    }
}
