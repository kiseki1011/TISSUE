package com.tissue.issuetype.application.port.in;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public interface IssueTypeUseCase {

    IssueTypeResponse create(String projectKey, CreateIssueTypeCommand cmd, WorkspaceMemberContext actorContext);

    void rename(String projectKey, Long issueTypeId, Name name, WorkspaceMemberContext actorContext);

    void update(String projectKey, Long issueTypeId, PatchIssueTypeCommand cmd, WorkspaceMemberContext actorContext);

    void delete(String projectKey, Long issueTypeId, WorkspaceMemberContext actorContext);
}
