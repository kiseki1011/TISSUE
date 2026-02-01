package com.tissue.workspace.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.member.domain.Member;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.domain.converter.ProjectJoinConfigListConverter;
import com.tissue.workspace.domain.enums.InvitationStatus;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Getter
public class Invitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole workspaceRole;

    @Convert(converter = ProjectJoinConfigListConverter.class)
    @Column(name = "project_configs", columnDefinition = "jsonb")
    private List<ProjectJoinConfig> projectConfigs = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Invitation() {
    }

    public static Invitation create(Workspace workspace, Member member,
        WorkspaceRole workspaceRole) {
        Invitation invitation = new Invitation();
        invitation.member = member;
        invitation.workspace = workspace;
        invitation.workspaceKey = workspace.getKey();
        invitation.status = InvitationStatus.PENDING;
        invitation.workspaceRole = workspaceRole;

        return invitation;
    }

    public void addProjectConfig(Project project, ProjectRole role) {
        this.projectConfigs.add(ProjectJoinConfig.of(project, role));
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
    }

    public boolean isProcessed() {
        return !isPending();
    }

    public boolean isPending() {
        return this.status == InvitationStatus.PENDING;
    }

    public boolean projectConfigsNotEmpty() {
        return !projectConfigs.isEmpty();
    }
}
