package com.tissue.api.project.application.port.in;

// TODO: ProjectMemberUseCase -> ProjectMemberCommandUseCase
public interface ProjectMemberCommandUseCase {

	// TODO: 그냥 ProjectCommandUseCase에 옮길까?

	// TODO: ProjectCommandResult addMembers(AddProjectMembersCommand cmd);
	//  - ProjectRole.ADMIN 이상
	//  - 흐름: Workspace의 모든 WorkspaceMember를 조회(getWorkspaceMembers API)해서 추가하고 싶은 멤버들을 한번에 모두 추가 허용

	// TODO: ProjectCommandResult kickMember(RemoveProjectMemberCommand cmd);
	//  - ProjectRole.ADMIN 이상

	// TODO: ProjectCommandResult leave(RemoveProjectMemberCommand cmd);

	// TODO: ProjectCommandResult join(JoinProjectCommand cmd);
	//  - 허용하는게 좋을까? Project에 참여 제한은 있어야 하지 않나?
	//  - 일단 WorkspaceRole.ADMIN 이상은 무조건 허용

	// TODO: ProjectCommandResult changeProjectRole(ChangeProjectRoleCommand cmd);
	//  - ProjectRole.ADMIN 이상
	//  - 본인 변경 불가
}
