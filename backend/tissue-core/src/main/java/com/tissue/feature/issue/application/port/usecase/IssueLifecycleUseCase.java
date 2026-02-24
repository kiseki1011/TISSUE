package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.BatchSoftDeleteCommand;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;

public interface IssueLifecycleUseCase {

    IssueCreateResponse create(ProjectIdentifier projectIdentifier, CreateIssueCommand cmd, Long actorMemberId);

    void delete(IssueIdentifier issueIdentifier, Long actorMemberId);

    void restore(IssueIdentifier issueIdentifier, Long actorMemberId);

    BatchOperationResponse batchSoftDelete(
            ProjectIdentifier projectIdentifier, BatchSoftDeleteCommand cmd, Long actorMemberId);
}
