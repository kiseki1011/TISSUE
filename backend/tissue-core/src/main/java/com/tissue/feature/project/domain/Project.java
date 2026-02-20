package com.tissue.feature.project.domain;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.project.domain.exception.ReservedProjectKeyException;
import com.tissue.feature.project.domain.policy.ProjectConstraintPolicy;
import com.tissue.feature.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "project",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"workspace_id", "project_key"})})
@SQLRestriction("soft_deleted = false")
public class Project extends SoftDeleteEntity {

    private static final Pattern KEY_PATTERN = Pattern.compile(ProjectConstraintPolicy.KEY_REGEX);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "workspace_key", nullable = false)
    private String workspaceKey;

    @Column(nullable = false)
    private String title;

    @Nullable
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectVisibility visibility;

    @Column(nullable = false)
    private Long issueNumber;

    @OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST)
    private List<IssueType> issueTypes = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST)
    private List<Workflow> workflows = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Project() {}

    public static Project create(Workspace workspace, String key, String title, @Nullable String description) {
        Project project = new Project();
        project.workspace = workspace;
        project.workspaceKey = workspace.getKey();
        project.issueNumber = 0L;
        project.setKey(key);
        project.title = title;
        project.description = Objects.requireNonNullElse(description, "");
        project.visibility = ProjectVisibility.PRIVATE;

        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspace.getKey());
        }

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
        validateEditable();
        this.title = title;
    }

    public void updateDescription(@Nullable String description) {
        validateEditable();
        this.description = Objects.requireNonNullElse(description, "");
    }

    public void updateVisibility(ProjectVisibility visibility) {
        this.visibility = visibility;
    }

    public Long generateNextIssueNumber() {
        return ++this.issueNumber;
    }

    public boolean isPublic() {
        return visibility == ProjectVisibility.PUBLIC;
    }

    public boolean isPrivate() {
        return visibility == ProjectVisibility.PRIVATE;
    }

    public void validateEditable() {
        if (this.isArchived()) {
            throw new ProjectArchivedException(workspaceKey, key);
        }
    }
}
