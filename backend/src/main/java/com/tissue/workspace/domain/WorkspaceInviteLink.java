package com.tissue.workspace.domain;

import com.tissue.global.converter.StringListConverter;
import com.tissue.global.entity.BaseEntity;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceInviteLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(nullable = false)
    private boolean active;

    /**
     * If expiredAt is null, the link is permanent.
     */
    @Nullable
    @Column(name = "expired_at")
    private Instant expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole workspaceRole;

    @Convert(converter = StringListConverter.class)
    @Column(name = "project_keys", columnDefinition = "JSONB")
    private List<String> projectKeys = new ArrayList<>();

    public static WorkspaceInviteLink create(
            Workspace workspace, String token, @Nullable WorkspaceRole role, @Nullable Instant expiredAt) {

        WorkspaceInviteLink link = new WorkspaceInviteLink();
        link.workspace = workspace;
        link.workspaceKey = workspace.getKey();
        link.token = token;
        link.workspaceRole = role != null ? role : WorkspaceRole.MEMBER;
        link.active = true;
        link.expiredAt = expiredAt;

        return link;
    }

    public void addProjectKey(String projectKey) {
        this.projectKeys.add(projectKey);
    }

    public void expire() {
        this.active = false;
    }

    public boolean isValid() {
        if (!active) {
            return false;
        }
        return !isExpired();
    }

    public boolean isDisabled() {
        return !active;
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
}
