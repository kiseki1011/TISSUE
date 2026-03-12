package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import com.tissue.feature.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface IssueUpdateUseCase {

    void updateCommonFields(IssueIdentifier iid, UpdateCommonFieldsCommand cmd, Long actorMemberId);

    void updateCustomFields(IssueIdentifier iid, Map<Long, Object> customFields, Long actorMemberId);

    void updateStoryPoint(IssueIdentifier iid, @Nullable Integer storyPoint, Long actorMemberId);

    void assignParent(IssueIdentifier iid, String parentIssueKey, Long actorMemberId);

    void removeParent(IssueIdentifier iid, Long actorMemberId);

    BatchOperationResponse batchAssignParent(ProjectIdentifier pid, BatchChangeParentCommand cmd, Long actorMemberId);

    // TODO: batchRemoveParent
}
