package com.tissue.api.project.domain;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.project.domain.enums.ProjectVisibility;
import com.tissue.api.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workspace.domain.Workspace;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("softDeleted = false")
@Table(
	name = "project",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"workspace_id", "project_key"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

	private final static String SPRINT_KEY_PREFIX = "SPRINT";

	// TODO: @Version 추가?

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
		@Nullable String description
	) {
		Project project = new Project();
		project.workspace = workspace;
		project.workspaceKey = workspace.getKey();

		project.setKey(key);
		project.title = title;
		project.description = nullToEmpty(description);

		project.visibility = ProjectVisibility.PRIVATE;
		project.defaultJoinRole = ProjectRole.VIEWER;

		return project;
	}

	private void setKey(@NonNull String key) {
		String upperKey = key.toUpperCase();
		if (ProjectKeyPrefixPolicy.isReserved(upperKey)) {
			// TODO: ReservedProjectKeyException
			throw new RuntimeException("Cannot use reserved key: " + upperKey);
		}
		// TODO: key 길이, 정규식 검증 -> 3~10자, 영문 대문자 + 숫자(선택 사항, 숫자는 무조건 뒤에 와야함)
		// TODO: CreateProjectRequest에도 bean validation 추가하기

		this.key = upperKey;
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void updateVisibility(@NonNull ProjectVisibility visibility) {
		this.visibility = visibility;
	}

	public void updateDefaultJoinRole(@NonNull ProjectRole defaultJoinRole) {
		if (defaultJoinRole.isEqualOrHigherThan(ProjectRole.ADMIN)) {
			// TODO: AdminDefaultJoinNotAllowedException, 더 좋은 이름이 있을까?
			throw new RuntimeException("Cannot set default join role as ADMIN.");
		}
		this.defaultJoinRole = defaultJoinRole;
	}

	public String generateNextIssueKey() {
		increaseIssueNumber();
		return "%s-%s".formatted(this.key, this.issueNumber);
	}

	private void increaseIssueNumber() {
		this.issueNumber++;
	}
}
