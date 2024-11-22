package com.uranus.taskmanager.api.workspace.domain;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseEntity;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.workspace.exception.InvalidMemberCountException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceMemberLimitExceededException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

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

	// @Version
	// private Long version;

	/**
	 * Todo
	 *  - memberCount에 캐시 적용 고려
	 *  - 낙관적락 적용 고려
	 *  -> memberCount 증가/감소가 들어가는 로직은 전부 예외를 잡고 재수행 로직을 적용해야 한다
	 */
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

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "workspace")
	private List<Invitation> invitations = new ArrayList<>();

	@Builder
	public Workspace(String code, String name, String description, String password) {
		this.code = code;
		this.name = name;
		this.description = description;
		this.password = password;
	}

	public void setCode(String code) {
		this.code = code;
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

	public void increaseMemberCount() {
		validateMemberLimit();
		this.memberCount++;
	}

	public void decreaseMemberCount() {
		validatePositiveMemberCount();
		this.memberCount--;
	}

	private void validateMemberLimit() {
		// Todo: 최대 멤버 참여 인원수 상수로 분리
		if (this.memberCount >= 500) {
			throw new WorkspaceMemberLimitExceededException();
		}
	}

	private void validatePositiveMemberCount() {
		if (this.memberCount <= 0) {
			throw new InvalidMemberCountException();
		}
	}

}
