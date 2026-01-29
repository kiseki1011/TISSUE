package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.AssignParentCommand;
import com.tissue.issue.application.dto.request.CreateIssueCommand;
import com.tissue.issue.application.dto.request.DeleteIssueCommand;
import com.tissue.issue.application.dto.request.RemoveParentCommand;
import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateCustomFieldsCommand;
import com.tissue.issue.application.dto.request.UpdateStoryPointCommand;
import com.tissue.issue.application.dto.response.IssueCreateResponse;

public interface IssueCommandUseCase {

    IssueCreateResponse create(CreateIssueCommand cmd);

    void updateCommonFields(UpdateCommonFieldsCommand cmd);

    void updateCustomFields(UpdateCustomFieldsCommand cmd);

    void updateStoryPoint(UpdateStoryPointCommand cmd);

    void assignParent(AssignParentCommand cmd);

    void removeParent(RemoveParentCommand cmd);

    void softDelete(DeleteIssueCommand cmd);

    // TODO: restore()
    //  - restore a soft deleted issue
    //  - must be ProjectRole.ADMIN
    //  - should i allow the author to restore it too?

    // TODO: batchChangeParent()
    //  - change or set a batch of issues parents
    //  - needs to consider validation logic

    // TODO: batchSoftDelete()
    //  - soft delete a batch if issues
    //  - needs to consider validation logic

    // TODO: cloneIssue() -> cant i just make this on the client side without making a api?
}
