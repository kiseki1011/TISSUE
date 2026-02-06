package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public interface ProjectUseCase {

    ProjectCommandResult create(CreateProjectCommand cmd, WorkspaceMemberContext actor);

    ProjectCommandResult update(String projectKey, UpdateProjectCommand cmd, WorkspaceMemberContext actor);

    ProjectCommandResult delete(String projectKey, WorkspaceMemberContext actor);

    // TODO: archive()

    // TODO: migrateProjectKey()
    //  - (Optional) 여유 있으면 구현하기

    // TODO: Project pagination api
    //  getProjects() vs getProjectPagination() vs getProjectPages() vs searchProjects()
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

    // TODO: getProjectDetail()
}
