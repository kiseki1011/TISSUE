package com.tissue.workspace.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.domain.converter.ProjectJoinConfigListConverter;
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
import lombok.NonNull;
import org.springframework.lang.Nullable;

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

    /** If expiredAt is null, the link is permanent. */
    @Column(nullable = true)
    private Instant expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole workspaceRole;

    @Convert(converter = ProjectJoinConfigListConverter.class)
    @Column(name = "project_configs", columnDefinition = "jsonb")
    private List<ProjectJoinConfig> projectConfigs = new ArrayList<>();

    public static WorkspaceInviteLink create(
            @NonNull Workspace workspace,
            @NonNull String token,
            @Nullable WorkspaceRole role,
            @Nullable Instant expiredAt) {
        WorkspaceInviteLink link = new WorkspaceInviteLink();
        link.workspace = workspace;
        link.workspaceKey = workspace.getKey();
        link.token = token;
        link.workspaceRole = role != null ? role : WorkspaceRole.MEMBER;
        link.active = true;
        link.expiredAt = expiredAt;

        return link;
    }

    public void addProjectConfig(Project project, ProjectRole role) {
        this.projectConfigs.add(ProjectJoinConfig.of(project, role));
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

    public boolean projectConfigsNotEmpty() {
        return !projectConfigs.isEmpty();
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
