package com.tissue.workspace.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.member.domain.Member;
import com.tissue.position.domain.Position;
import com.tissue.team.domain.Team;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
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
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("softDeleted = false")
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

    // TODO: consider using nickname or profileName
    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String email;

    // TODO: consider adding a bio field
    // private String bio;

    public static WorkspaceMember create(Member member, Workspace workspace, WorkspaceRole role) {
        WorkspaceMember workspaceMember = new WorkspaceMember();
        workspaceMember.workspace = workspace;
        workspaceMember.workspaceKey = workspace.getKey();
        workspaceMember.member = member;
        workspaceMember.email = member.getEmail();
        // TODO: after refactoring Member so the "name" field is required, use it for the default
        // displayName
        //  member.getUsername() -> member.getName()
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

    public void changeRoleTo(WorkspaceRole newRole) {
        if (role == newRole) {
            return;
        }
        if (newRole == WorkspaceRole.OWNER) {
            throw WorkspaceExceptions.cannotChangeRoleToOwner();
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

    public boolean roleIsLowerThan(WorkspaceRole role) {
        return this.role.isLowerThan(role);
    }

    public boolean roleIsEqualOrHigherThan(WorkspaceRole role) {
        return this.role.isEqualOrHigherThan(role);
    }

    public void addPosition(Position position) {
        WorkspaceMemberPosition.create(this, position);
    }

    public void removePosition(Position position) {
        WorkspaceMemberPosition wmp =
                this.workspaceMemberPositions.stream()
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
        WorkspaceMemberTeam wmp =
                this.workspaceMemberTeams.stream()
                        .filter(w -> w.getTeam().equals(team))
                        .findFirst()
                        .orElse(null);

        if (wmp != null) {
            this.workspaceMemberTeams.remove(wmp);
            team.getWorkspaceMemberTeams().remove(wmp);
        }
    }
}
