package com.tissue.feature.projecttemplate.domain;

import com.tissue.feature.projecttemplate.domain.config.TemplateConfig;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(name = "project_template")
public class ProjectTemplate extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_payload", columnDefinition = "jsonb", nullable = false)
    private TemplateConfig configPayload;

    @SuppressWarnings("NullAway.Init")
    protected ProjectTemplate() {}

    public static ProjectTemplate create(
            Workspace workspace, String name, @Nullable String description, TemplateConfig configPayload) {
        ProjectTemplate template = new ProjectTemplate();
        template.workspace = workspace;
        template.name = name;
        template.description = Objects.requireNonNullElse(description, "");
        template.configPayload = configPayload;
        return template;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }
}
