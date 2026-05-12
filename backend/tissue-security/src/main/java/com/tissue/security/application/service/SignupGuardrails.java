package com.tissue.security.application.service;

import static com.tissue.security.domain.exception.AuthenticationErrorCode.EMAIL_SIGNUP_DISABLED;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.security.config.DeploymentProperties;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.domain.exception.SignupBlockedNoWorkspaceException;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupGuardrails {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
    private final SignupProperties signupProperties;
    private final DeploymentProperties deploymentProperties;

    public boolean isFirstUser() {
        return memberRepository.count() == 0;
    }

    public void ensureSignupAllowed() {
        ensureSignupFeatureEnabled();
        ensureWorkspaceContextAllowsSignup();
    }

    public void autoJoinSingleWorkspaceIfApplicable(Member member) {
        if (isMultiTenant()) {
            return;
        }
        if (hasNoActiveWorkspace()) {
            return;
        }
        if (hasMultipleActiveWorkspaces()) {
            return;
        }
        joinAsMember(member, getSoleActiveWorkspace());
    }

    private void ensureSignupFeatureEnabled() {
        if (!signupProperties.isEnabled()) {
            throw new ForbiddenException(EMAIL_SIGNUP_DISABLED);
        }
    }

    private void ensureWorkspaceContextAllowsSignup() {
        if (isMultiTenant()) {
            return;
        }
        if (isFirstUser()) {
            return;
        }
        if (hasNoActiveWorkspace()) {
            throw new SignupBlockedNoWorkspaceException();
        }
    }

    private boolean isMultiTenant() {
        return deploymentProperties.isMultiTenant();
    }

    private boolean hasNoActiveWorkspace() {
        return workspaceRepository.countBySoftDeletedFalse() == 0L;
    }

    private boolean hasMultipleActiveWorkspaces() {
        return workspaceRepository.countBySoftDeletedFalse() > 1L;
    }

    private Workspace getSoleActiveWorkspace() {
        return workspaceRepository.findFirstBySoftDeletedFalseOrderByIdAsc().orElseThrow();
    }

    private void joinAsMember(Member member, Workspace workspace) {
        WorkspaceMember workspaceMember = WorkspaceMember.create(member, workspace, WorkspaceRole.MEMBER);
        workspaceMemberCommandRepository.save(workspaceMember);
    }
}
