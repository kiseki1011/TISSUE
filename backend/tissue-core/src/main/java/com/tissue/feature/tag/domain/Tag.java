package com.tissue.feature.tag.domain;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "tag",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tag_project_id_normalized",
                    columnNames = {"project_id", "normalized_name"})
        })
public class Tag extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

    @Embedded
    private Name name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @SuppressWarnings("NullAway.Init")
    protected Tag() {}

    public static Tag create(Project project, Name name, @Nullable String description, ColorType color) {
        Tag tag = new Tag();
        tag.project = project;
        tag.ensureEditable();
        tag.projectKey = project.getKey();
        tag.name = name;
        tag.description = Objects.requireNonNullElse(description, "");
        tag.color = color;
        return tag;
    }

    public void rename(Name name) {
        ensureEditable();
        this.name = name;
    }

    public void updateColor(ColorType color) {
        ensureEditable();
        this.color = color;
    }

    public void updateDescription(@Nullable String description) {
        ensureEditable();
        this.description = Objects.requireNonNullElse(description, "");
    }

    private void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getKey());
        }
    }
}
