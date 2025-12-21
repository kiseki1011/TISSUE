package com.tissue.security.authorization.workspace;

import org.springframework.stereotype.Component;

import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.domain.WorkspaceInviteLink;

import lombok.RequiredArgsConstructor;

// TODO: integrate into WorkspaceSecurityGuard
@Component
@RequiredArgsConstructor
public class WorkspaceInviteLinkSecurityGuard {

	private final WorkspaceLinkQueryRepository linkRepository;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;

	public boolean canExpire(String workspaceKey, String token, Long memberId) {
		var linkOpt = linkRepository.findByToken(token);
		if (linkOpt.isEmpty()) {
			return false;
		}

		WorkspaceInviteLink link = linkOpt.get();

		if (workspaceNotMatch(workspaceKey, link)) {
			return false;
		}
		if (isCreator(memberId, link)) {
			return true;
		}

		return workspaceSecurityGuard.isAdmin(link.getWorkspaceKey(), memberId);
	}

	private boolean isCreator(Long memberId, WorkspaceInviteLink link) {
		return link.getCreatedBy().equals(memberId);
	}

	private boolean workspaceNotMatch(String workspaceKey, WorkspaceInviteLink link) {
		return !link.getWorkspaceKey().equals(workspaceKey);
	}
}
