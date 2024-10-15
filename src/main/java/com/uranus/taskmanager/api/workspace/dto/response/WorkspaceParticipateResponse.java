package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkspaceParticipateResponse {

	/*
	 * Todo:
	 *  - 참여한 워크스페이스의 상세 정보
	 *  - 이름
	 *  - 설명
	 *  - 코드
	 *  - 인원(Box 사용) - 인원 계산을 서비스 vs 엔티티에서?
	 *  - 인원 계산에 캐시 적용 찾아보기(변동 시점에만 갱신하기)
	 *  - 현재 내 별칭
	 *  - 현재 내 권한
	 *  - fromEntity(): Workspace, WorkspaceMember -> WorkspaceParticipateResponse
	 */
	private final String name;
	private final String description;
	private final String code;
	private final int headcount;
	private final String nickname;
	private final WorkspaceRole myRole;
	private final boolean isAlreadyMember;

	@Builder
	public WorkspaceParticipateResponse(String name, String description, String code, int headcount, String nickname,
		WorkspaceRole myRole, boolean isAlreadyMember) {
		this.name = name;
		this.description = description;
		this.code = code;
		this.headcount = headcount;
		this.nickname = nickname;
		this.myRole = myRole;
		this.isAlreadyMember = isAlreadyMember;
	}

	public static WorkspaceParticipateResponse from(Workspace workspace, WorkspaceMember workspaceMember,
		int headcount, boolean isAlreadyMember) {
		return WorkspaceParticipateResponse.builder()
			.name(workspace.getName())
			.description(workspace.getDescription())
			.code(workspace.getCode())
			.headcount(headcount)
			.nickname(workspaceMember.getNickname())
			.myRole(workspaceMember.getRole())
			.isAlreadyMember(isAlreadyMember)
			.build();
	}
}
