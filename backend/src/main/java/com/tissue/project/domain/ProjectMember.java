package com.tissue.project.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.project.domain.enums.ProjectRole;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("softDeleted = false")
@Table(
        name = "project_member",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"project_id", "workspace_member_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    public static ProjectMember create(
            @NonNull Project project,
            @NonNull WorkspaceMember workspaceMember,
            @NonNull ProjectRole role) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.project = project;
        projectMember.projectKey = project.getKey();
        projectMember.workspaceKey = project.getWorkspaceKey();

        projectMember.workspaceMember = workspaceMember;
        projectMember.memberId = workspaceMember.getMemberId();

        projectMember.role = role;

        return projectMember;
    }

    public void changeRole(@NonNull ProjectRole newRole) {
        this.role = newRole;
    }

    public void remove() {
        softDelete();
    }

    public String getDisplayName() {
        return workspaceMember.getDisplayName();
    }
}
