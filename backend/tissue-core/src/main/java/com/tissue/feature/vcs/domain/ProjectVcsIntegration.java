package com.tissue.feature.vcs.domain;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.global.crypto.EncryptedStringConverter;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "project_vcs_integration",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_vcs_integration_project_provider",
                        columnNames = {"project_key", "vcs_provider"}))
public class ProjectVcsIntegration extends HardDeleteEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "vcs_provider", nullable = false)
    private VcsProvider provider;

    @Column(name = "project_key", nullable = false)
    private String projectKey;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    @Column(name = "sync_enabled", nullable = false)
    private boolean active;

    @SuppressWarnings("NullAway.Init")
    protected ProjectVcsIntegration() {}

    public static ProjectVcsIntegration create(VcsProvider provider, String projectKey, String webhookSecret) {
        ProjectVcsIntegration integration = new ProjectVcsIntegration();
        integration.provider = provider;
        integration.projectKey = projectKey;
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

    public boolean isInactive() {
        return !active;
    }
}
