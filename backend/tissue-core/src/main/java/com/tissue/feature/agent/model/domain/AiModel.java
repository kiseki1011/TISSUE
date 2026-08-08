package com.tissue.feature.agent.model.domain;

import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A globally-managed catalog entry for an AI model (ex: "claude-opus-4-8"). Agents reference one so
 * the model they run is a curated value rather than free text. Managed by system admins.
 */
@Entity
@Getter
@Table(
        name = "ai_model",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ai_model_name",
                    columnNames = {"normalized_name"})
        })
public class AiModel extends HardDeleteEntity {

    @Version
    private Long version;

    @Embedded
    private Name name;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @SuppressWarnings("NullAway.Init")
    protected AiModel() {}

    public static AiModel create(Name name, @Nullable String description, ColorType color) {
        AiModel model = new AiModel();
        model.name = name;
        model.description = Objects.requireNonNullElse(description, "");
        model.color = color;

        return model;
    }

    public String getName() {
        return name.toString();
    }

    public void rename(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void updateColor(ColorType color) {
        this.color = color;
    }
}
