package com.tissue.project.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.workspace.domain.WorkspaceMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class ProjectMember extends BaseEntity {

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

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @SuppressWarnings("NullAway.Init")
    protected ProjectMember() {}

    public static ProjectMember create(Project project, WorkspaceMember workspaceMember) {

        ProjectMember projectMember = new ProjectMember();
        projectMember.project = project;
        projectMember.projectKey = project.getKey();
        projectMember.workspaceKey = project.getWorkspaceKey();
        projectMember.workspaceMember = workspaceMember;
        projectMember.memberId = workspaceMember.getMemberId();

        return projectMember;
    }

    public void remove() {
        softDelete();
    }
}
