package com.tissue.api.workspace.domain.model;

import static com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole.*;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.global.key.KeyGenerator;
import com.tissue.api.global.key.KeyPrefixPolicy;
import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.workspace.exception.WorkspaceOwnershipRequiredException;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
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

	// TODO: @NonNull(롬복), @Nullable 추가
	public static Workspace create(
		String key,
		String name,
		String description,
		String password,
		String issueKeyPrefix,
		Member member
	) {
		Workspace workspace = new Workspace();
		workspace.key = key;
		workspace.name = name;
		workspace.description = description;
		workspace.password = password;
		workspace.issueKeyPrefix = issueKeyPrefix; // updateIssueKeyPrefix?

		workspace.workspaceMembers.add(WorkspaceMember.create(member, workspace, OWNER));

		return workspace;
	}

	public WorkspaceMember addMember(Member member, WorkspaceRole role) {
		WorkspaceMember workspaceMember = WorkspaceMember.create(member, this, role);
		this.workspaceMembers.add(workspaceMember);

		return workspaceMember;
	}

	public void removeMember(WorkspaceMember workspaceMember) {
		this.workspaceMembers.remove(workspaceMember);
	}

	public void transferOwnership(WorkspaceMember owner, WorkspaceMember newOwner) {
		if (!owner.isOwner()) {
			throw new WorkspaceOwnershipRequiredException("Needs to be OWNER to transfer ownership.",
				key, owner.getMemberId(), owner.getRole());
		}
		owner.changeRoleTo(ADMIN);
		newOwner.changeRoleToOwner();
	}

	public void setKey(String key) {
		this.key = key;
	}

	// TODO: Issue key prefix(이슈키 접두사)의 길이는 영문 대문자 3 ~ 24자
	// TODO: 추후에 Project 애그리거트 개발을 완료하면, {projectKey}-{issueNumber}로 이슈키가 만들어지도록 할거임.
	//  한마디로, projectKey가 이슈키 접두사가 되도록
	public void updateIssueKeyPrefix(String newPrefix) {
		if (newPrefix == null) {
			newPrefix = KeyPrefixPolicy.ISSUE;
		}

		newPrefix = newPrefix.toUpperCase();
		if (KeyPrefixPolicy.isReserved(newPrefix)) {
			// TODO: ReservedProjectKeyException
			throw new RuntimeException("Cannot use reserved key prefix: " + newPrefix);
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

	// public void ensureCanAddMember(WorkspacePolicy workspacePolicy) {
	// 	workspacePolicy.ensureWithinMemberLimit(this);
	// }

	public int getMemberCount() {
		return workspaceMembers.size();
	}
}
