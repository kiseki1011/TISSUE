package com.tissue.api.global.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tissue.api.security.authentication.MemberUserDetails;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberAuditorProvider implements AuditorAware<Long> {

	@Override
	public Optional<Long> getCurrentAuditor() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			return Optional.empty();
		}

		boolean unauthenticated = !authentication.isAuthenticated();
		if (unauthenticated) {
			return Optional.empty();
		}

		if (authentication.getPrincipal() instanceof MemberUserDetails userDetails) {
			return Optional.ofNullable(userDetails.getMemberId());
		}

		return Optional.empty();
	}
}
