package com.tissue.feature.workspace.domain;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class WorkspaceInviteLink extends HardDeleteEntity {

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    /**
     * If expiredAt is null, the link is permanent.
     */
    @Nullable
    @Column(name = "expired_at")
    private Instant expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole workspaceRole;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "project_keys", columnDefinition = "jsonb")
    private List<String> projectKeys = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceInviteLink() {}

    public static WorkspaceInviteLink create(
            Workspace workspace, String token, @Nullable WorkspaceRole role, @Nullable Instant expiredAt) {

        WorkspaceInviteLink link = new WorkspaceInviteLink();
        link.workspace = workspace;
        link.ensureEditable();
        link.workspaceKey = workspace.getKey();
        link.token = token;
        link.workspaceRole = role != null ? role : WorkspaceRole.MEMBER;
        link.expiredAt = expiredAt;

        return link;
    }

    public void addProjectKey(String projectKey) {
        this.projectKeys.add(projectKey);
    }

    public boolean isValid() {
        return !isExpired();
    }

    public boolean projectKeysNotEmpty() {
        return !projectKeys.isEmpty();
    }

    private boolean isExpired() {
        if (isPermanentLink()) {
            return false;
        }
        return Instant.now().isAfter(expiredAt);
    }

    private boolean isPermanentLink() {
        return expiredAt == null;
    }

    private void ensureEditable() {
        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspace.getKey());
        }
    }
}
