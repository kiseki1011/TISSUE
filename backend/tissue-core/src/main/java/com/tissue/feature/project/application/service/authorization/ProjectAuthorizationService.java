package com.tissue.feature.project.application.service.authorization;

import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_JOIN_NOT_ALLOWED;
import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED;
import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_MANAGER_REQUIRED;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    public void requireSystemAdmin(ProjectMember actor) {
        if (actor.getMember().hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        throw new ForbiddenException(MemberErrorCode.SYSTEM_ADMIN_REQUIRED);
    }

    public void requireProjectManager(ProjectMember actor) {
        if (actor.getMember().hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        if (actor.isManager()) {
            return;
        }
        throw new ForbiddenException(PROJECT_MANAGER_REQUIRED);
    }

    public void requireJoinPermission(Member actor, Project project) {
        if (actor.hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ForbiddenException(PROJECT_JOIN_NOT_ALLOWED);
    }

    public void requireHigherRole(ProjectMember actor, ProjectMember target) {
        if (actor.getMember().hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        if (!actor.isManager()) {
            throw new ForbiddenException(PROJECT_MANAGER_REQUIRED);
        }
        if (target.isManager()) {
            throw new ForbiddenException(PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED);
        }
    }
}
