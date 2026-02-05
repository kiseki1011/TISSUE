package com.tissue.workspace.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.position.domain.Position;
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
                    name = "uk_workspace_member_position",
                    columnNames = {"workspace_member_id", "position_id"})
        })
@Getter
public class WorkspaceMemberPosition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceMemberPosition() {}

    public WorkspaceMemberPosition(WorkspaceMember workspaceMember, Position position) {
        this.workspaceMember = workspaceMember;
        this.position = position;
    }

    public static WorkspaceMemberPosition create(WorkspaceMember workspaceMember, Position position) {
        WorkspaceMemberPosition wmp = new WorkspaceMemberPosition(workspaceMember, position);
        workspaceMember.getWorkspaceMemberPositions().add(wmp);
        position.getWorkspaceMemberPositions().add(wmp);
        return wmp;
    }
}
