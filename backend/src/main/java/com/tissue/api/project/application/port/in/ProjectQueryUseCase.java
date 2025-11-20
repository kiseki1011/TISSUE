package com.tissue.api.project.application.port.in;

public interface ProjectQueryUseCase {

	// TODO: Project pagination api - getProjects()
	//  default
	//   - 20 projects
	//   - joinedDate DESC 참여 안한 project가 후순위
	//  search by
	//   - createdDate (범위 검색 가능)
	//   - name
	//   - description (optional)
	//   - project key
	//   - 내가 참여 중인 Project를 필터링 가능
	//   - 현재 활성화된 Sprint가 존재하는 Project를 필터링 가능 (optional)
	//  sort by
	//   - joinedDate DESC
	//   - createdDate DESC
	//   - total issue numbers (optional)
	//   - total project members (optional)

	// TODO: ProjectMember 관련 조회는 ProjectMemberQueryUseCase를 만들어서 분리하는게 좋을까?
	// TODO: ProjectMemberDetail getProjectMemberDetail(String workspaceKey, String projectKey, Long memberId);

	// TODO: ProjectMember pagination api
	//  search by
	//   - name -> workspaceMember.member.name
	//   - username -> workspaceMember.member.username
	//   - display name -> workspaceMember.displayName
	//   - ProjectRole
	//   - 해당 Project에서 활성화된 Issue(initial 또는 terminal state가 아닌 issue)에 참여 중인 ProjectMember들 (optional)
	//   - 해당 Project에 활성화된 Sprint에 참여중인 ProjectMember들 (optional)
	//  sort by
	//   - name alphabet
	//   - displayName alphabet
	//   - ProjectRole(default, 높은순)
	//   - 참여 순
}
