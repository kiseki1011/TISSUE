package com.tissue.vcs.domain;

import com.tissue.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("soft_deleted = false")
public class WorkspaceVcsIntegration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    @Column(name = "github_sync_enabled", nullable = false)
    private boolean githubSyncEnabled;

    public static WorkspaceVcsIntegration create(String workspaceKey, String webhookSecret) {
        WorkspaceVcsIntegration integration = new WorkspaceVcsIntegration();
        integration.workspaceKey = workspaceKey;
        integration.webhookSecret = webhookSecret;
        // TODO: default false로 할까?
        integration.githubSyncEnabled = true;
        return integration;
    }

    public void toggleSync(boolean enabled) {
        this.githubSyncEnabled = enabled;
    }

    public void rotateSecret(String newSecret) {
        this.webhookSecret = newSecret;
    }
}
