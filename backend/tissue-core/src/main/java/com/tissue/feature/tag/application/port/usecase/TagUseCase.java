package com.tissue.feature.tag.application.port.usecase;

import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.dto.request.UpdateTagCommand;
import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.dto.response.TagResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;

public interface TagUseCase {

    TagResponse create(ProjectIdentifier projectIdentifier, CreateTagCommand cmd, Long actorMemberId);

    void rename(ProjectIdentifier projectIdentifier, Long tagId, String newName, Long actorMemberId);

    void update(ProjectIdentifier projectIdentifier, Long tagId, UpdateTagCommand cmd, Long actorMemberId);

    void delete(ProjectIdentifier projectIdentifier, Long tagId, Long actorMemberId);

    List<TagDetail> getTagsByProject(ProjectIdentifier projectIdentifier, Long actorMemberId);
}
