package com.tissue.project.domain;

import com.tissue.global.entity.SoftDeleteEntity;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.workspace.domain.WorkspaceMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
        name = "project_member",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"project_id", "workspace_member_id"})})
@Getter
public class ProjectMember extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // TODO: 아무리 편의성이라지만, denormalization을 위해서 여기에 추가해서 사용하는거 거부감이 듬.
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
        projectMember.validateEditable();
        projectMember.projectKey = project.getKey();
        projectMember.workspaceKey = project.getWorkspaceKey();
        projectMember.workspaceMember = workspaceMember;
        projectMember.memberId = workspaceMember.getMemberId();
        projectMember.role = ProjectRole.MEMBER;

        return projectMember;
    }

    public static ProjectMember createOwner(Project project, WorkspaceMember workspaceMember) {
        ProjectMember owner = create(project, workspaceMember);
        owner.changeRole(ProjectRole.MANAGER);
        return owner;
    }

    public void validateEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }

    public void changeRole(ProjectRole role) {
        this.role = role;
    }
}
