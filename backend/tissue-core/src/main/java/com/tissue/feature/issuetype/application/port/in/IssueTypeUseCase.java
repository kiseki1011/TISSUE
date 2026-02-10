package com.tissue.feature.issuetype.application.port.in;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.shared.vo.Name;

public interface IssueTypeUseCase {

    IssueTypeResponse create(String projectKey, CreateIssueTypeCommand cmd, WorkspaceMemberContext actorContext);

    void rename(String projectKey, Long issueTypeId, Name name, WorkspaceMemberContext actorContext);

    void update(String projectKey, Long issueTypeId, PatchIssueTypeCommand cmd, WorkspaceMemberContext actorContext);

    void delete(String projectKey, Long issueTypeId, WorkspaceMemberContext actorContext);
}
