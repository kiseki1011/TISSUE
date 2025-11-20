package com.tissue.api.workspace.application.port.in;

public interface WorkspaceMemberQueryUseCase {

	// TODO: WorkspaceMember pagination api
	//  search by
	//   - name
	//   - username
	//   - display name
	//   - WorkspaceRole
	//  sort by
	//   - name alphabet
	//   - WorkspaceRole(default, 높은순)
	//   - 참여 순
	//  each item schema
	//   - name
	//   - username
	//   - display name
	//   - WorkspaceRole
	//   - 참여 중인 project들(projectKey-projectRole)

	// TODO: getWorkspaceMemberDetail
	//   - name
	//   - username
	//   - display name
	//   - WorkspaceRole
	//   - email (고민중)
	//   - 참여 date time
	//   - 참여 중인 project들(projectKey-projectRole)
}
