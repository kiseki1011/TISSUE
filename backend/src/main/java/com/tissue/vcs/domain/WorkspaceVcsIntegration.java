package com.tissue.vcs.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.vcs.domain.enums.VcsProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Getter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspaceKey", "vcsProvider"}))
public class WorkspaceVcsIntegration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vcs_provider", nullable = false)
    private VcsProvider provider;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    @Column(name = "sync_enabled", nullable = false)
    private boolean active;

    @SuppressWarnings("NullAway.Init")
    protected WorkspaceVcsIntegration() {
    }

    public static WorkspaceVcsIntegration create(VcsProvider provider, String workspaceKey,
        String webhookSecret) {
        WorkspaceVcsIntegration integration = new WorkspaceVcsIntegration();
        integration.provider = provider;
        integration.workspaceKey = workspaceKey;
        integration.webhookSecret = webhookSecret;
        integration.active = true;
        return integration;
    }

    public void toggleSync(boolean enabled) {
        this.active = enabled;
    }

    public void rotateSecret(String newSecret) {
        this.webhookSecret = newSecret;
    }
}
