package com.tissue.issuetype.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;

import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueTypeUseCase {

    @PreAuthorize(REQUIRES_PROJECT_MEMBER)
    IssueTypeResponse create(CreateIssueTypeCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void rename(RenameIssueTypeCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void update(PatchIssueTypeCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void delete(DeleteIssueTypeCommand cmd);
}
