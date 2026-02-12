package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;

public interface IssueTypeUseCase {

    IssueTypeResponse create(ProjectIdentifier projectIdentifier, CreateIssueTypeCommand cmd, Long memberId);

    void rename(ProjectIdentifier projectIdentifier, Long issueTypeId, Name name, Long memberId);

    void update(ProjectIdentifier projectIdentifier, Long issueTypeId, PatchIssueTypeCommand cmd, Long memberId);

    void delete(ProjectIdentifier projectIdentifier, Long issueTypeId, Long memberId);
}
