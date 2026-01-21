package com.tissue.workspace.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.member.domain.Member;
import com.tissue.position.domain.Position;
import com.tissue.team.domain.Team;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.CannotChangeRoleToOwnerException;
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
public class WorkspaceMember extends BaseEntity {

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

    // TODO: consider adding a bio field
    // private String bio;

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceMember() {}

    public static WorkspaceMember create(Member member, Workspace workspace, WorkspaceRole role) {
        WorkspaceMember workspaceMember = new WorkspaceMember();
        workspaceMember.workspace = workspace;
        workspaceMember.workspaceKey = workspace.getKey();
        workspaceMember.member = member;
        workspaceMember.displayName = member.getName();
        workspaceMember.role = role;
        return workspaceMember;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public boolean isOwner() {
        return this.role == WorkspaceRole.OWNER;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateRole(WorkspaceRole newRole) {
        if (role == newRole) {
            return;
        }
        // TODO: 어차피 서비스 계층에서 authorization service로 검증을 하는데, 굳이 이걸 체크해야하나?
        if (newRole == WorkspaceRole.OWNER) {
            throw new CannotChangeRoleToOwnerException();
        }
        this.role = newRole;
    }

    protected void changeRoleToOwner() {
        this.role = WorkspaceRole.OWNER;
    }

    public void addPosition(Position position) {
        WorkspaceMemberPosition.create(this, position);
    }

    public void removePosition(Position position) {
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
        WorkspaceMemberTeam.create(this, team);
    }

    public void removeTeam(Team team) {
        WorkspaceMemberTeam wmp = this.workspaceMemberTeams.stream()
                .filter(w -> w.getTeam().equals(team))
                .findFirst()
                .orElse(null);

        if (wmp != null) {
            this.workspaceMemberTeams.remove(wmp);
            team.getWorkspaceMemberTeams().remove(wmp);
        }
    }
}
