package com.tissue.feature.project.domain;

import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.project.domain.exception.ReservedProjectKeyException;
import com.tissue.feature.project.domain.policy.ProjectConstraintPolicy;
import com.tissue.feature.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.regex.Pattern;
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

    private static final Pattern KEY_PATTERN = Pattern.compile(ProjectConstraintPolicy.KEY_REGEX);

    @Column(name = "project_key", nullable = false, updatable = false)
    private String key;

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
        project.setKey(key);
        project.title = title;
        project.description = Objects.requireNonNullElse(description, "");
        project.visibility = ProjectVisibility.PUBLIC;

        return project;
    }

    private void setKey(String key) {
        String upperKey = key.toUpperCase();

        validateKeyFormat(upperKey);

        if (ProjectKeyPrefixPolicy.isReserved(upperKey)) {
            throw new ReservedProjectKeyException(key);
        }
        this.key = upperKey;
    }

    private void validateKeyFormat(String key) {
        if (key.length() < ProjectConstraintPolicy.KEY_MIN_LENGTH
                || key.length() > ProjectConstraintPolicy.KEY_MAX_LENGTH) {
            throw new BadRequestException(ProjectErrorCode.INVALID_PROJECT_KEY_FORMAT);
        }

        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException(ProjectErrorCode.INVALID_PROJECT_KEY_FORMAT);
        }
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
            throw new ProjectArchivedException(key);
        }
    }
}
