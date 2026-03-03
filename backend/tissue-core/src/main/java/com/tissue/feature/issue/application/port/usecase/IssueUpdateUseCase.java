package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface IssueUpdateUseCase {

    void updateCommonFields(IssueIdentifier issueIdentifier, UpdateCommonFieldsCommand cmd, Long actorMemberId);

    void updateCustomFields(IssueIdentifier issueIdentifier, Map<Long, Object> customFields, Long actorMemberId);

    void updateStoryPoint(IssueIdentifier issueIdentifier, @Nullable Integer storyPoint, Long actorMemberId);

    void assignParent(IssueIdentifier issueIdentifier, String parentIssueKey, Long actorMemberId);

    void removeParent(IssueIdentifier issueIdentifier, Long actorMemberId);

    BatchOperationResponse batchAssignParent(
            ProjectIdentifier projectIdentifier, BatchChangeParentCommand cmd, Long actorMemberId);

    // TODO: batchRemoveParent
}
