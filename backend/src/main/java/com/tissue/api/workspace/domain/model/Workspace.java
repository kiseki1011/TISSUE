package com.tissue.api.workspace.domain.model;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.global.key.KeyGenerator;
import com.tissue.api.global.key.KeyPrefixPolicy;
import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "workspace_id")
	private Long id;

	@Column(name = "workspace_key", unique = true, nullable = false)
	private String key;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	private String password;

	@Column(nullable = false)
	private String issueKeyPrefix;

	@Column(nullable = false)
	private Integer issueNumber = 0;

	@Column(nullable = false)
	private Integer sprintNumber = 0;

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Invitation> invitations = new ArrayList<>();

	@OneToMany(mappedBy = "workspace")
	private List<Sprint> sprints = new ArrayList<>();

	@Builder
	public Workspace(
		String key,
		String name,
		String description,
		String password,
		String issueKeyPrefix
	) {
		this.key = key;
		this.name = name;
		this.description = description;
		this.password = password;
		updateIssueKeyPrefix(issueKeyPrefix);
	}

	public void setKey(String key) {
		this.key = key;
	}

	// TODO: Issue key prefix must be 3 ~ 24 characters (only en)
	//  Use WorkspacePolicy.ensureKeyPrefixValidLength
	public void updateIssueKeyPrefix(String newPrefix) {
		if (newPrefix == null) {
			newPrefix = KeyPrefixPolicy.ISSUE;
		}

		newPrefix = newPrefix.toUpperCase();
		if (KeyPrefixPolicy.isReserved(newPrefix)) {
			throw new InvalidOperationException("Cannot use reserved key prefix: " + newPrefix);
		}

		this.issueKeyPrefix = newPrefix;
	}

	public String generateCurrentIssueKey() {
		increaseIssueNumber();
		return KeyGenerator.generateIssueKey(issueKeyPrefix, issueNumber);
	}

	public String generateSprintKey() {
		increaseSprintNumber();
		return KeyGenerator.generateSprintKey(sprintNumber);
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

	public void increaseIssueNumber() {
		this.issueNumber++;
	}

	public void increaseSprintNumber() {
		this.sprintNumber++;
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

	public void ensureCanAddMember(WorkspacePolicy workspacePolicy) {
		workspacePolicy.ensureWithinMemberLimit(this);
	}

	public int getMemberCount() {
		return workspaceMembers.size();
	}
}
