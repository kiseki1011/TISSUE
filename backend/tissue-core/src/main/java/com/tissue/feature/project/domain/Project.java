package com.tissue.feature.project.domain;

import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.project.domain.vo.ProjectKey;
import com.tissue.shared.entity.SoftDeleteEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "project",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_project_key",
                    columnNames = {"project_key"})
        })
@SQLRestriction("soft_deleted = false")
public class Project extends SoftDeleteEntity {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "project_key", nullable = false, updatable = false))
    private ProjectKey key;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private ProjectVisibility visibility;

    @Column(name = "issue_number", nullable = false)
    private Long issueNumber;

    @Column(name = "sprint_number", nullable = false)
    private Long sprintNumber;

    // TODO: Add recentSummary - a summary of the recent activity of this project. (optional)

    @SuppressWarnings("NullAway.Init")
    protected Project() {}

    public static Project create(String key, String title, @Nullable String description) {
        Project project = new Project();
        project.issueNumber = 0L;
        project.sprintNumber = 0L;
        project.key = ProjectKey.of(key);
        project.title = title;
        project.description = Objects.requireNonNullElse(description, "");
        project.visibility = ProjectVisibility.PUBLIC;

        return project;
    }

    public String getKey() {
        return key.getValue();
    }

    public void updateTitle(String title) {
        ensureEditable();
        this.title = title;
    }

    public void updateDescription(@Nullable String description) {
        ensureEditable();
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void updateVisibility(ProjectVisibility visibility) {
        this.visibility = visibility;
    }

    public Long generateNextIssueNumber() {
        return ++this.issueNumber;
    }

    public Long generateNextSprintNumber() {
        return ++this.sprintNumber;
    }

    public boolean isPublic() {
        return visibility == ProjectVisibility.PUBLIC;
    }

    public boolean isPrivate() {
        return visibility == ProjectVisibility.PRIVATE;
    }

    public void ensureEditable() {
        if (this.isArchived()) {
            throw new ProjectArchivedException(getKey());
        }
    }
}
