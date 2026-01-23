package com.tissue.vcs.domain;

import com.tissue.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@SQLRestriction("soft_deleted = false")
public class WorkspaceVcsIntegration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    @Column(name = "sync_enabled", nullable = false)
    private boolean syncEnabled;

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceVcsIntegration() {}

    public static WorkspaceVcsIntegration create(String workspaceKey, String webhookSecret) {
        WorkspaceVcsIntegration integration = new WorkspaceVcsIntegration();
        integration.workspaceKey = workspaceKey;
        integration.webhookSecret = webhookSecret;
        // TODO: default false로 할까?
        integration.syncEnabled = true;
        return integration;
    }

    public void toggleSync(boolean enabled) {
        this.syncEnabled = enabled;
    }

    public void rotateSecret(String newSecret) {
        this.webhookSecret = newSecret;
    }
}
