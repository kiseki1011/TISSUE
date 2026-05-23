package com.tissue.feature.tag.application.port.usecase;

import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.dto.request.UpdateTagCommand;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.shared.dto.ProjectIdentifier;

public interface TagCommandUseCase {

    TagResponse create(ProjectIdentifier pid, CreateTagCommand cmd, Long actorMemberId);

    void update(String workspaceKey, Long tagId, UpdateTagCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long tagId, Long actorMemberId);
}
