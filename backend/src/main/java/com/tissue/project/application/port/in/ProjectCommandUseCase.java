package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.request.CreateProjectCommand;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;

public interface ProjectCommandUseCase {

    ProjectCommandResult create(CreateProjectCommand cmd);

    ProjectCommandResult update(UpdateProjectCommand cmd);

    ProjectCommandResult delete(DeleteProjectCommand cmd);

    // TODO: archive()
    // TODO: migrateProjectKey()
    //  - 선택사항. 여유 있으면 구현하기.
    //  - softDelete 상태에서도 가능하도록?
}
