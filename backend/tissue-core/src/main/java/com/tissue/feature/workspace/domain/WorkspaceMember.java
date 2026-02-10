package com.tissue.feature.workspace.domain;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.CannotChangeRoleToOwnerException;
import com.tissue.feature.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.shared.entity.SoftDeleteEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

@Entity
@Getter
public class WorkspaceMember extends SoftDeleteEntity {

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

    @OneToMany(mappedBy = "workspaceMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkspaceMemberPosition> workspaceMemberPositions = new HashSet<>();

    @OneToMany(mappedBy = "workspaceMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkspaceMemberTeam> workspaceMemberTeams = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_role", nullable = false)
    private WorkspaceRole role;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceMember() {}

    public static WorkspaceMember create(Member member, Workspace workspace, WorkspaceRole role) {
        WorkspaceMember workspaceMember = new WorkspaceMember();
        workspaceMember.workspace = workspace;
        workspaceMember.workspaceKey = workspace.getKey();
        workspaceMember.member = member;
        workspaceMember.displayName = member.getName();
        workspaceMember.role = role;
        workspaceMember.ensureEditable();

        return workspaceMember;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public boolean isOwner() {
        return this.role == WorkspaceRole.OWNER;
    }

    public void updateDisplayName(String displayName) {
        ensureEditable();
        this.displayName = displayName;
    }

    public void updateRole(WorkspaceRole newRole) {
        ensureEditable();
        if (role == newRole) {
            return;
        }
        if (newRole == WorkspaceRole.OWNER) {
            throw new CannotChangeRoleToOwnerException();
        }
        this.role = newRole;
    }

    protected void changeRoleToOwner() {
        this.role = WorkspaceRole.OWNER;
    }

    public void addPosition(Position position) {
        ensureEditable();
        WorkspaceMemberPosition.create(this, position);
    }

    public void removePosition(Position position) {
        ensureEditable();
        WorkspaceMemberPosition wmp = this.workspaceMemberPositions.stream()
                .filter(w -> w.getPosition().equals(position))
                .findFirst()
                .orElse(null);

        if (wmp != null) {
            this.workspaceMemberPositions.remove(wmp);
            position.getWorkspaceMemberPositions().remove(wmp);
        }
    }

    public void addTeam(Team team) {
        ensureEditable();
        WorkspaceMemberTeam.create(this, team);
    }

    public void removeTeam(Team team) {
        ensureEditable();
        WorkspaceMemberTeam wmp = this.workspaceMemberTeams.stream()
                .filter(w -> w.getTeam().equals(team))
                .findFirst()
                .orElse(null);

        if (wmp != null) {
            this.workspaceMemberTeams.remove(wmp);
            team.getWorkspaceMemberTeams().remove(wmp);
        }
    }

    public void ensureEditable() {
        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspaceKey);
        }
    }
}
