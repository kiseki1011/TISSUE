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
 * Loads the actor's {@link ProjectMember} for project commands that need authorization.
 *
 * <p>Same loaders as {@link ProjectMemberFinder}, except a non-member who is a system {@code ADMIN}
 * gets a transient(not-persisted) override membership (see {@link ProjectMember#createOverride}) instead
 * of a 404. This lets an {@link SystemRole#ADMIN} operate on a project they haven't joined. Other non-members
 * still get {@link ProjectMemberNotFoundException}. For reads and membership-only commands, use
 * {@link ProjectMemberFinder} instead.
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
