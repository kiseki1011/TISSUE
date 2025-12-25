package com.tissue.security.authorization.workspace;

import static com.tissue.workspace.domain.enums.WorkspaceRole.*;

import org.springframework.stereotype.Component;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceSecurityGuard {

	private final WorkspaceLinkQueryRepository linkRepository;
	private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

	public boolean isMember(String workspaceKey, MemberUserDetails userDetails) {
		return userDetails.hasWorkspaceRole(workspaceKey, MEMBER);
	}

	public boolean isAdmin(String workspaceKey, MemberUserDetails userDetails) {
		return userDetails.hasWorkspaceRole(workspaceKey, ADMIN);
	}

	public boolean isOwner(String workspaceKey, MemberUserDetails userDetails) {
		return userDetails.hasWorkspaceRole(workspaceKey, OWNER);
	}

	public boolean isSelfModification(String workspaceKey, Long memberId, MemberUserDetails userDetails) {
		return isAdmin(workspaceKey, userDetails)
			|| memberId.equals(userDetails.getMemberId());
	}

	public boolean hasHigherRoleThanTarget(String workspaceKey, Long targetMemberId, MemberUserDetails userDetails) {
		if (targetMemberId.equals(userDetails.getMemberId())) {
			return false;
		}
		if (isNotAdmin(workspaceKey, userDetails)) {
			return false;
		}
		return hasHigherRoleThan(workspaceKey, targetMemberId, userDetails);
	}

	public boolean canGrantRole(String workspaceKey, WorkspaceRole grantRole, MemberUserDetails userDetails) {
		if (grantRole == WorkspaceRole.OWNER) {
			return false;
		}
		if (isNotAdmin(workspaceKey, userDetails)) {
			return false;
		}
		return userDetails.hasWorkspaceRole(workspaceKey, grantRole);
	}

	public boolean canEditInviteLink(String workspaceKey, String token, MemberUserDetails userDetails) {
		return userDetails.hasWorkspaceRole(workspaceKey, WorkspaceRole.ADMIN)
			|| isLinkCreator(token, userDetails);
	}

	private boolean isLinkCreator(String token, MemberUserDetails userDetails) {
		return linkRepository.findByToken(token)
			.map(link -> link.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}

	private boolean hasHigherRoleThan(String workspaceKey, Long targetMemberId, MemberUserDetails userDetails) {
		return workspaceMemberQueryRepository.findByMember_IdAndWorkspaceKey(targetMemberId, workspaceKey)
			.map(target -> userDetails.hasWorkspaceRole(workspaceKey, target.getRole()))
			.orElse(false);
	}

	private boolean isNotAdmin(String workspaceKey, MemberUserDetails userDetails) {
		return !userDetails.hasWorkspaceRole(workspaceKey, ADMIN);
	}
}
