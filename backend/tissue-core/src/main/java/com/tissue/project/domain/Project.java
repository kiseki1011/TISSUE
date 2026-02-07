package com.tissue.project.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.project.domain.exception.ReservedProjectKeyException;
import com.tissue.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.exception.WorkspaceArchivedException;
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
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "project",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"workspace_id", "project_key"})})
@SQLRestriction("soft_deleted = false")
public class Project extends BaseEntity {

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
        // TODO: validate key length(3~10),
        //  pattern(letters + number, number must come behind if used)
        String upperKey = key.toUpperCase();
        if (ProjectKeyPrefixPolicy.isReserved(upperKey)) {
            throw new ReservedProjectKeyException(key);
        }
        this.key = upperKey;
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

    // TODO: use atomic update("select for update") for issue creation
    public Long generateNextIssueNumber() {
        return this.issueNumber++;
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
