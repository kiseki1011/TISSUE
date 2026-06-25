package com.tissue.feature.project.domain;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
        name = "project_member",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_project_member_project_id_member_id",
                    columnNames = {"project_id", "member_id"})
        })
@Getter
public class ProjectMember extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false)
    private ProjectRole role;

    @SuppressWarnings("NullAway.Init")
    protected ProjectMember() {}

    public static ProjectMember create(Project project, Member member) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.project = project;
        projectMember.ensureEditable();
        projectMember.projectKey = project.getKey();
        projectMember.member = member;
        projectMember.role = ProjectRole.MEMBER;

        return projectMember;
    }

    public static ProjectMember createManager(Project project, Member member) {
        ProjectMember owner = create(project, member);
        owner.changeRole(ProjectRole.MANAGER);
        return owner;
    }

    /**
     * Transient (not-persisted) membership letting a {@link SystemRole#ADMIN} act on a project they
     * haven't joined.
     *
     * <p>Role is a placeholder {@code MEMBER}. Authorization decides on the actor's system role instead.
     * <b>Must not be persisted.</b> Only {@link ProjectAccessResolver} should create this.
     */
    public static ProjectMember createOverride(Project project, Member member) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.project = project;
        projectMember.projectKey = project.getKey();
        projectMember.member = member;
        projectMember.role = ProjectRole.MEMBER;
        return projectMember;
    }

    public void changeRole(ProjectRole role) {
        this.role = role;
    }

    public boolean isManager() {
        return this.role == ProjectRole.MANAGER;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public String getDisplayName() {
        return member.getName();
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getKey());
        }
    }
}
