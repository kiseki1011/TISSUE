package com.tissue.project.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.project.domain.exception.ProjectExceptions;
import com.tissue.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workspace.domain.Workspace;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

@Entity
@SQLRestriction("softDeleted = false")
@Table(
        name = "project",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"workspace_id", "project_key"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole defaultJoinRole;

    @Column(nullable = false)
    private Integer issueNumber = 0;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssueType> issueTypes = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Workflow> workflows = new ArrayList<>();

    public static Project create(
            @NonNull Workspace workspace,
            @NonNull String key,
            @NonNull String title,
            @Nullable String description) {
        Project project = new Project();
        project.workspace = workspace;
        project.workspaceKey = workspace.getKey();
        project.setKey(key);
        project.title = title;
        project.description = description;
        project.visibility = ProjectVisibility.PRIVATE;
        project.defaultJoinRole = ProjectRole.VIEWER;

        return project;
    }

    private void setKey(@NonNull String key) {
        // TODO: validate key length(3~10), pattern(letters + number, number must come behind if
        // used)
        // TODO: dd bean validation for CreateProjectRequest

        String upperKey = key.toUpperCase();
        if (ProjectKeyPrefixPolicy.isReserved(upperKey)) {
            throw ProjectExceptions.reservedKey(upperKey);
        }
        this.key = upperKey;
    }

    public void updateTitle(@NonNull String title) {
        this.title = title;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }

    public void updateVisibility(@NonNull ProjectVisibility visibility) {
        this.visibility = visibility;
    }

    public void updateDefaultJoinRole(@NonNull ProjectRole defaultJoinRole) {
        if (defaultJoinRole.isEqualOrHigherThan(ProjectRole.ADMIN)) {
            throw ProjectExceptions.invalidDefaultJoinRole(defaultJoinRole);
        }
        this.defaultJoinRole = defaultJoinRole;
    }

    public String generateNextIssueKey() {
        this.issueNumber++;
        return "%s-%s".formatted(this.key, this.issueNumber);
    }
}
