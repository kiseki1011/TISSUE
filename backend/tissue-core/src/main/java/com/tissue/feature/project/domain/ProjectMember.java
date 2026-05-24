package com.tissue.feature.project.domain;

import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
        name = "project_member",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"project_id", "workspace_member_id"})},
        indexes = {
            @Index(
                    name = "idx_project_member_member_id",
                    columnList = "member_id")
        })
@Getter
public class ProjectMember extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false)
    private ProjectRole role;

    @SuppressWarnings("NullAway.Init")
    protected ProjectMember() {}

    public static ProjectMember create(Project project, WorkspaceMember workspaceMember) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.project = project;
        projectMember.ensureEditable();
        projectMember.projectKey = project.getKey();
        projectMember.workspaceKey = project.getWorkspaceKey();
        projectMember.workspaceMember = workspaceMember;
        projectMember.memberId = workspaceMember.getMemberId();
        projectMember.role = ProjectRole.MEMBER;

        return projectMember;
    }

    public static ProjectMember createManager(Project project, WorkspaceMember workspaceMember) {
        ProjectMember owner = create(project, workspaceMember);
        owner.changeRole(ProjectRole.MANAGER);
        return owner;
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }

    public void changeRole(ProjectRole role) {
        this.role = role;
    }

    public boolean isManager() {
        return this.role == ProjectRole.MANAGER;
    }
}
