package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface IssueCommandUseCase {

    IssueCreateResponse create(ProjectIdentifier projectIdentifier, CreateIssueCommand cmd, Long memberId);

    void updateCommonFields(IssueIdentifier issueIdentifier, UpdateCommonFieldsCommand cmd, Long memberId);

    void updateCustomFields(IssueIdentifier issueIdentifier, Map<Long, Object> customFields, Long memberId);

    void updateStoryPoint(IssueIdentifier issueIdentifier, @Nullable Integer storyPoint, Long memberId);

    void assignParent(IssueIdentifier issueIdentifier, String parentIssueKey, Long memberId);

    void removeParent(IssueIdentifier issueIdentifier, Long memberId);

    void delete(IssueIdentifier issueIdentifier, Long memberId);

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
