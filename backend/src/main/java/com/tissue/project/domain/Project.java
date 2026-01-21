package com.tissue.project.domain;

import com.tissue.common.entity.BaseEntity;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.project.domain.exception.InvalidDefaultJoinRoleException;
import com.tissue.project.domain.exception.ReservedProjectKeyException;
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
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@SQLRestriction("soft_deleted = false")
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

    @Nullable
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole defaultJoinRole;

    @Column(nullable = false)
    private Long issueNumber;

    @OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST)
    private List<IssueType> issueTypes = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST)
    private List<Workflow> workflows = new ArrayList<>();

    public static Project create(Workspace workspace, String key, String title, @Nullable String description) {
        Project project = new Project();
        project.workspace = workspace;
        project.workspaceKey = workspace.getKey();
        project.issueNumber = 0L;
        project.setKey(key);
        project.title = title;
        project.description = description;
        project.visibility = ProjectVisibility.PRIVATE;
        project.defaultJoinRole = ProjectRole.VIEWER;

        return project;
    }

    private void setKey(String key) {
        // TODO: validate key length(3~10),
        //  pattern(letters + number, number must come behind if used)
        // TODO: bean validation for CreateProjectRequest

        String upperKey = key.toUpperCase();
        if (ProjectKeyPrefixPolicy.isReserved(upperKey)) {
            throw new ReservedProjectKeyException(key);
        }
        this.key = upperKey;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }

    public void updateVisibility(ProjectVisibility visibility) {
        this.visibility = visibility;
    }

    public void updateDefaultJoinRole(ProjectRole defaultJoinRole) {
        if (defaultJoinRole.isEqualOrHigherThan(ProjectRole.ADMIN)) {
            throw new InvalidDefaultJoinRoleException(defaultJoinRole);
        }
        this.defaultJoinRole = defaultJoinRole;
    }

    // TODO: use atomic update("select for update") for issue creation
    public Long generateNextIssueNumber() {
        return this.issueNumber++;
    }

    public boolean isPublic() {
        return visibility == ProjectVisibility.PUBLIC;
    }
}
