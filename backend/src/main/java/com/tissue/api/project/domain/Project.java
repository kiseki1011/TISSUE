package com.tissue.api.project.domain;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.domain.policy.ProjectKeyPrefixPolicy;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * TODO
 *  - 인덱스 설정
 *  - workspace + key 유일성 제약
 */
@Entity
@SQLRestriction("softDeleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

	private final static String SPRINT_KEY_PREFIX = "SPRINT";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(name = "workspace_key", nullable = false)
	private String workspaceKey;

	@Column(name = "project_key", nullable = false)
	private String key;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private Integer issueNumber = 0;

	@Column(nullable = false)
	private Integer sprintNumber = 0;

	// TODO: Sprint와 양방향 관계 고려. 하지만 웬만하면 단방향으로 설계하는게 좋을 것 같음.
	//  Sprint의 수가 1000개가 넘어가는 경우 getSprints()에 대한 성능 부담이 걱정됨.

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueType> issueTypes = new ArrayList<>();

	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Workflow> workflows = new ArrayList<>();

	public static Project create(
		@NonNull Workspace workspace,
		@NonNull String key,
		@NonNull String name,
		@Nullable String description
	) {
		Project project = new Project();
		project.workspace = workspace;
		project.workspaceKey = workspace.getKey();
		project.updateKey(key);
		project.name = name;
		project.description = nullToEmpty(description);

		return project;
	}

	// TODO: 그런데 만약 프로젝트의 키를 업데이트하면 기존 이슈 키들은 어떻게 변해야 할까?
	//  다른 프로젝트 관리 시스템에서는 어떻게 처리하는지 궁금.
	public void updateKey(@NonNull String newKey) {
		String upperKey = newKey.toUpperCase();
		if (ProjectKeyPrefixPolicy.isReserved(upperKey)) {
			// TODO: ReservedProjectKeyException
			throw new RuntimeException("Cannot use reserved key: " + upperKey);
		}
		// TODO: Project 키 길이, 정규식 검증 (예: 2~10자 영문 대문자)

		this.key = upperKey;
	}

	public void updateName(@NonNull String name) {
		this.name = name;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public String generateNextIssueKey() {
		increaseIssueNumber();
		return "%s-%s".formatted(this.key, this.issueNumber);
	}

	public String generateSprintKey() {
		increaseSprintNumber();
		return "%s-%s".formatted(SPRINT_KEY_PREFIX, this.sprintNumber);
	}

	private void increaseIssueNumber() {
		this.issueNumber++;
	}

	private void increaseSprintNumber() {
		this.sprintNumber++;
	}

	private void delete() {
		softDelete();
	}

	// TODO: getActiveSprints (Sprint와 양방향 관계인 경우에만 추가)
}
