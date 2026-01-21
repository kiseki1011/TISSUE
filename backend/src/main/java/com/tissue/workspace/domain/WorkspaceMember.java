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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Column(nullable = false)
    private WorkspaceRole role;

    @Column(nullable = false)
    private String displayName;

    // TODO: 제거하는게 좋을까? 아니면 워크스페이스별 email 두는게 좋을까?
    @Column(nullable = false)
    private String email;

    // TODO: consider adding a bio field
    // private String bio;

    public static WorkspaceMember create(Member member, Workspace workspace, WorkspaceRole role) {
        WorkspaceMember workspaceMember = new WorkspaceMember();
        workspaceMember.workspace = workspace;
        workspaceMember.workspaceKey = workspace.getKey();
        workspaceMember.member = member;
        // TODO: member의 email이 변경되는 경우 어떻게?
        workspaceMember.email = member.getEmail();
        // TODO: after refactoring Member so the "name" field is required, use it for the default?
        workspaceMember.displayName = member.getUsername();
        workspaceMember.role = role;

        return workspaceMember;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public String getUsername() {
        return member.getUsername();
    }

    public String getEmail() {
        return member.getEmail();
    }

    public void updateRole(WorkspaceRole newRole) {
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

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isOwner() {
        return this.role == WorkspaceRole.OWNER;
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
