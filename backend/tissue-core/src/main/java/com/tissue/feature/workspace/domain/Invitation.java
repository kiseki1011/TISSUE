package com.tissue.feature.workspace.domain;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
public class Invitation extends HardDeleteEntity {

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
    private WorkspaceRole workspaceRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "project_keys", columnDefinition = "jsonb")
    private List<String> projectKeys = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Invitation() {}

    public static Invitation create(Workspace workspace, Member member, WorkspaceRole workspaceRole) {
        Invitation invitation = new Invitation();
        invitation.member = member;
        invitation.workspace = workspace;
        invitation.ensureEditable();
        invitation.workspaceKey = workspace.getKey();
        invitation.workspaceRole = workspaceRole;

        return invitation;
    }

    public void addProjectKey(String projectKey) {
        projectKeys.add(projectKey);
    }

    public boolean projectKeysNotEmpty() {
        return !projectKeys.isEmpty();
    }

    public void ensureEditable() {
        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspace.getKey());
        }
    }
}
