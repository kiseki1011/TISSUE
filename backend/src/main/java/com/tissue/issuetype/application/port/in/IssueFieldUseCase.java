package com.tissue.issuetype.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;

import com.tissue.issuetype.application.dto.request.AddOptionCommand;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteOptionCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.RenameOptionCommand;
import com.tissue.issuetype.application.dto.request.ReorderOptionsCommand;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.issuetype.application.dto.response.ReorderedOptionsResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueFieldUseCase {

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    IssueFieldResponse create(CreateIssueFieldCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void rename(RenameIssueFieldCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void update(PatchIssueFieldCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void delete(DeleteIssueFieldCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    IssueFieldResponse addOption(AddOptionCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void renameOption(RenameOptionCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    ReorderedOptionsResponse reorderOptions(ReorderOptionsCommand cmd);

    @PreAuthorize(REQUIRES_ISSUE_TYPE_EDIT_PERMISSION)
    void deleteOption(DeleteOptionCommand cmd);
}
