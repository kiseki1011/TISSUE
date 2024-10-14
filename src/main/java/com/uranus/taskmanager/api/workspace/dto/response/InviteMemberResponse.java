package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class InviteMemberResponse {

	private final String code;
	// Todo: 어차피 PENDING 상태인게 확실한데 상태를 굳이 응답으로 보여줘야할까?
	private final InvitationStatus status;
	// Todo: loginId, email을 묶어서 표현한 내부 클래스, InvitedMembersResponse에서 사용하는 경우 클래스 분리 고려
	private final InvitedMember invitedMember;
	// private final String loginId;
	// private final String email;

	// Todo: 초대한 사람 추가(Inviter)

	// @Builder
	// public InviteMemberResponse(String code, String loginId, String email, InvitationStatus status) {
	// 	this.code = code;
	// 	this.loginId = loginId;
	// 	this.email = email;
	// 	this.status = status;
	// }

	@Builder
	public InviteMemberResponse(String code, InvitationStatus status, InvitedMember invitedMember) {
		this.code = code;
		this.status = status;
		this.invitedMember = invitedMember;
	}

	public static InviteMemberResponse fromEntity(Invitation invitation) {
		return InviteMemberResponse.builder()
			.code(invitation.getWorkspace().getCode())
			// .loginId(invitation.getMember().getLoginId())
			// .email(invitation.getMember().getEmail())
			.invitedMember(InvitedMember.fromEntity(invitation))
			.status(invitation.getStatus())
			.build();
	}
}
