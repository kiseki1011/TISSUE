package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public interface ProjectUseCase {

    ProjectCommandResult create(CreateProjectCommand cmd, WorkspaceMemberContext actor);

    ProjectCommandResult update(UpdateProjectCommand cmd, WorkspaceMemberContext actor, String projectKey);

    ProjectCommandResult delete(DeleteProjectCommand cmd, WorkspaceMemberContext actor);

    // TODO: archive()
    // TODO: migrateProjectKey()
    //  - 선택사항. 여유 있으면 구현하기.
    //  - softDelete 상태에서도 가능하도록?
}
