package com.tissue.feature.project.application.service.finder;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the actor {@link ProjectMember} for authorization-bearing project-scoped commands, with
 * system-admin operator override.
 *
 * <p>Mirrors {@link ProjectMemberFinder}'s loader shapes, but instead of rejecting a non-member it
 * grants a system {@code ADMIN} + a transient override membership (see {@link ProjectMember#createOverride}).
 * This makes the documented "system ADMIN can operator-override any project-scoped action" actually
 * hold for an admin who is not a member of the project. Ordinary (non-admin) non-members still get
 * {@link ProjectMemberNotFoundException} (404), preserving existing behavior.
 *
 * <p>Use this only on authorization-bearing command paths (where a {@code requireXxx} role/ownership
 * check follows). Reads and membership-only commands keep their membership-only semantics via
 * {@link ProjectMemberFinder}.
 */
@Component
@RequiredArgsConstructor
public class ProjectAccessResolver {

    private final ProjectMemberQueryRepository queryRepository;
    private final ProjectFinder projectFinder;
    private final MemberFinder memberFinder;

    /**
     * Mirrors {@link ProjectMemberFinder#getBy}
     * (project already loaded).
     */
    public ProjectMember resolveBy(Project project, Long memberId) {
        return queryRepository.findByProjectAndMemberId(project, memberId).orElseGet(() -> {
            Member member = memberFinder.getActiveById(memberId);
            if (!member.hasAtLeast(SystemRole.ADMIN)) {
                throw new ProjectMemberNotFoundException(project.getKey(), memberId);
            }
            return ProjectMember.createOverride(project, member);
        });
    }

    /**
     * Mirrors {@link ProjectMemberFinder#getByProjectKey}
     * (member graph loaded).
     */
    public ProjectMember resolveByProjectKey(String projectKey, Long memberId) {
        return queryRepository
                .findWithMemberByProjectKeyAndMemberId(projectKey, memberId)
                .orElseGet(() -> overrideOrThrow(projectKey, memberId));
    }

    /**
     * Mirrors {@link ProjectMemberFinder#getWithProject}
     * (project graph loaded).
     */
    public ProjectMember resolveWithProject(String projectKey, Long memberId) {
        return queryRepository
                .findWithProjectByProjectKeyAndMemberId(projectKey, memberId)
                .orElseGet(() -> overrideOrThrow(projectKey, memberId));
    }

    private ProjectMember overrideOrThrow(String projectKey, Long memberId) {
        Member member = memberFinder.getActiveById(memberId);
        if (!member.hasAtLeast(SystemRole.ADMIN)) {
            throw new ProjectMemberNotFoundException(projectKey, memberId);
        }
        Project project = projectFinder.getByProjectKey(projectKey);
        return ProjectMember.createOverride(project, member);
    }
}
