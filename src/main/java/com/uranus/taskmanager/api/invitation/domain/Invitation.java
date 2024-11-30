package com.uranus.taskmanager.api.invitation.domain;

import com.uranus.taskmanager.api.common.entity.BaseEntity;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "INVITATION_ID")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_ID", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@Enumerated(EnumType.STRING)
	private InvitationStatus status;

	@Builder
	public Invitation(Member member, Workspace workspace, InvitationStatus status) {
		this.member = member;
		this.workspace = workspace;
		this.status = status;
		this.workspaceCode = workspace.getCode();
	}

	public static Invitation addInvitation(Member member, Workspace workspace, InvitationStatus status) {
		Invitation invitation = Invitation.builder()
			.member(member)
			.workspace(workspace)
			.status(status)
			.build();

		member.getInvitations().add(invitation);
		workspace.getInvitations().add(invitation);

		return invitation;
	}

	public static Invitation createPendingInvitation(Workspace workspace, Member member) {
		return Invitation.builder()
			.workspace(workspace)
			.member(member)
			.status(InvitationStatus.PENDING)
			.build();
	}

	public WorkspaceMember addWorkspaceMember() {
		return WorkspaceMember.addCollaboratorWorkspaceMember(
			this.member,
			this.workspace
		);
	}

	public WorkspaceMember accept() {
		changeStatus(InvitationStatus.ACCEPTED);
		return addWorkspaceMember();
	}

	public void reject() {
		changeStatus(InvitationStatus.REJECTED);
	}

	private void changeStatus(InvitationStatus status) {
		this.status = status;
	}
}
