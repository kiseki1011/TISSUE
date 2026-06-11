package com.tissue.feature.project.domain;

import com.tissue.feature.member.domain.Member;
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
     * Builds a transient (never-persisted) membership representing a system {@code ADMIN}+ operator
     * override for a project they do not belong to. The role is {@code MEMBER}, but the role/ownership
     * authorization services short-circuit on the actor's system role, so this {@code MEMBER} role is
     * never the deciding factor. Skips {@code ensureEditable()} (no real membership is being created).
     *
     * <p><b>MUST NOT be persisted.</b> Only {@link ProjectAccessResolver} should create this.
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
