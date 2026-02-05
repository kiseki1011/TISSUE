package com.tissue.issuetype.application.port.in;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.project.application.dto.ProjectMemberContext;

public interface IssueTypeUseCase {

    IssueTypeResponse create(CreateIssueTypeCommand cmd, ProjectMemberContext actorContext);

    void rename(Long issueTypeId, Name name, ProjectMemberContext actorContext);

    void update(Long issueTypeId, PatchIssueTypeCommand cmd, ProjectMemberContext actorContext);

    void delete(Long issueTypeId, ProjectMemberContext actorContext);
}
