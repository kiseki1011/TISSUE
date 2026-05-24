package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;

public interface IssueTypeQueryUseCase {

    List<IssueTypeSummary> getProjectIssueTypes(ProjectIdentifier pid, Long actorMemberId);

    IssueTypeDetail getIssueTypeDetail(String workspaceKey, Long issueTypeId, Long actorMemberId);
}
