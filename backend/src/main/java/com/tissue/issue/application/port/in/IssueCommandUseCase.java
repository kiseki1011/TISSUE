package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.application.dto.response.IssueCreateResponse;
import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface IssueCommandUseCase {

    IssueCreateResponse create(CreateIssueCommand cmd, ProjectMemberContext actorContext);

    void updateCommonFields(String issueKey, UpdateCommonFieldsCommand cmd, ProjectMemberContext actorContext);

    void updateCustomFields(String issueKey, Map<Long, Object> customFields, ProjectMemberContext actorContext);

    void updateStoryPoint(String issueKey, @Nullable Integer storyPoint, ProjectMemberContext actorContext);

    void assignParent(String issueKey, String parentIssueKey, ProjectMemberContext actorContext);

    void removeParent(String issueKey, ProjectMemberContext actorContext);

    void delete(String issueKey, ProjectMemberContext actorContext);

    // TODO: restore()
    //  - restore a soft deleted issue
    //  - projectEditPermission
    //  - should i allow the author to restore it too?

    // TODO: batchChangeParent()
    //  - change or set a batch of issues parents
    //  - needs to consider validation logic

    // TODO: batchSoftDelete()
    //  - soft delete a batch if issues
    //  - projectEditPermission
    //  - needs to consider validation logic
}
