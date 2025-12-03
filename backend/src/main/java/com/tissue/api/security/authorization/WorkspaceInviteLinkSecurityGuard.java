package com.tissue.api.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.api.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.api.workspace.domain.WorkspaceInviteLink;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceInviteLinkSecurityGuard {

	private final WorkspaceLinkQueryRepository linkRepository;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;

	public boolean canExpire(String token, Long memberId) {
		var linkOpt = linkRepository.findByToken(token);
		// TODO: orElseThrow로 LinkNotFoundException(404) 고려
		if (linkOpt.isEmpty()) {
			return false;
		}
		WorkspaceInviteLink link = linkOpt.get();

		if (link.getCreatedBy().equals(memberId)) {
			return true;
		}

		return workspaceSecurityGuard.isAdmin(link.getWorkspaceKey(), memberId);
	}
}
