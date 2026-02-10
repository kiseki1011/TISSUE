package com.tissue.feature.workspace.domain;

import com.tissue.feature.organization.team.domain.Team;
import com.tissue.shared.entity.HardDeleteEntity;
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
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_workspace_member_team",
                    columnNames = {"workspace_member_id", "team_id"})
        })
@Getter
public class WorkspaceMemberTeam extends HardDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceMemberTeam() {}

    public WorkspaceMemberTeam(WorkspaceMember workspaceMember, Team team) {
        this.workspaceMember = workspaceMember;
        this.team = team;
    }

    public static WorkspaceMemberTeam create(WorkspaceMember workspaceMember, Team team) {
        WorkspaceMemberTeam wmt = new WorkspaceMemberTeam(workspaceMember, team);
        workspaceMember.getWorkspaceMemberTeams().add(wmt);
        team.getWorkspaceMemberTeams().add(wmt);
        return wmt;
    }
}
