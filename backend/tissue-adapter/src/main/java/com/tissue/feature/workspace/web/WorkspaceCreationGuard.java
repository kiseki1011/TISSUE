package com.tissue.feature.workspace.web;

import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.security.config.DeploymentProperties;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceCreationGuard {

    private final DeploymentProperties deploymentProperties;

    public void ensureAllowed(MemberDetails memberDetails) {
        if (deploymentProperties.isMultiTenant()) {
            return;
        }
        if (memberDetails.hasRole(SystemRole.ADMIN)) {
            return;
        }
        throw new ForbiddenException(WorkspaceErrorCode.WORKSPACE_CREATE_ADMIN_ONLY);
    }
}
