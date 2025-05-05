package com.tissue.api.workspace.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;
import com.tissue.api.team.domain.Team;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

	// Todo: 추후 낙관적 락 적용
	// @Version
	// private Long version;

	private static final int MAX_MEMBER_COUNT = 500;
	private static final String DEFAULT_KEY_PREFIX = "ISSUE";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "WORKSPACE_ID")
	private Long id;

	@Column(unique = true, nullable = false)
	private String code;

	@Column(nullable = false)
	private String name;
	@Column(nullable = false)
	private String description;

	private String password;

	@Column(nullable = false)
	private int memberCount = 0;

	@Column(nullable = false)
	private String issueKeyPrefix;

	@Column(nullable = false)
	private Integer nextIssueNumber = 1;

	@Column(nullable = false)
	private Integer nextSprintNumber = 1;

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Team> teams = new ArrayList<>();

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Invitation> invitations = new ArrayList<>();

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Issue> issues = new ArrayList<>();

	@OneToMany(mappedBy = "workspace")
	private List<Sprint> sprints = new ArrayList<>();

	@Builder
	public Workspace(
		String code,
		String name,
		String description,
		String password,
		String issueKeyPrefix
	) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.password = password;
		this.issueKeyPrefix = toUpperCaseOrDefault(issueKeyPrefix);
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void updateIssueKeyPrefix(String issueKeyPrefix) {
		this.issueKeyPrefix = toUpperCaseOrDefault(issueKeyPrefix);
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public Set<ColorType> getUsedTeamColors() {
		return this.teams.stream()
			.map(Team::getColor)
			.collect(Collectors.toSet());
	}

	/*
	 * Todo
	 *  - Workspace의 책임인가?
	 *  - 그냥 Issue에서 workspace.getKeyPrefix + workspace.getNextIssueNumber로 처리하면 안되나?
	 */
	public String getIssueKey() {
		return String.format("%s-%d", issueKeyPrefix, nextIssueNumber);
	}

	public void increaseNextIssueNumber() {
		this.nextIssueNumber++;
	}

	public void increaseNextSprintNumber() {
		this.nextSprintNumber++;
	}

	public void increaseMemberCount() {
		validateMemberLimit();
		this.memberCount++;
	}

	public void decreaseMemberCount() {
		validatePositiveMemberCount();
		this.memberCount--;
	}

	private String toUpperCaseOrDefault(String keyPrefix) {
		return keyPrefix != null ? keyPrefix.toUpperCase() : DEFAULT_KEY_PREFIX;
	}

	public boolean hasActiveSprintExcept(Sprint excludedSprint) {
		return sprints.stream()
			.filter(sprint -> !sprint.equals(excludedSprint))
			.anyMatch(sprint -> sprint.getStatus() == SprintStatus.ACTIVE);
	}

	public boolean hasActiveSprint() {
		return sprints.stream()
			.anyMatch(sprint -> sprint.getStatus() == SprintStatus.ACTIVE);
	}

	private void validateMemberLimit() {
		if (memberCount >= MAX_MEMBER_COUNT) {
			throw new InvalidOperationException(String.format(
				"Maximum number of workspace members reached. Workspace member limit: %d",
				MAX_MEMBER_COUNT));
		}
	}

	private void validatePositiveMemberCount() {
		if (memberCount <= 0) {
			throw new InvalidOperationException("Number of workspace members cannot go below 1.");
		}
	}
}
