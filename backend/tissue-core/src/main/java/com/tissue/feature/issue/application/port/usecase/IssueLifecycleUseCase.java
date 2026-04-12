package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.BatchDeleteCommand;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.shared.dto.BatchOperationResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;

public interface IssueLifecycleUseCase {

    IssueCreateResponse create(ProjectIdentifier pid, CreateIssueCommand cmd, Long actorMemberId);

    void delete(IssueIdentifier iid, Long actorMemberId);

    void restore(IssueIdentifier iid, Long actorMemberId);

    BatchOperationResponse batchDelete(ProjectIdentifier pid, BatchDeleteCommand cmd, Long actorMemberId);
}
