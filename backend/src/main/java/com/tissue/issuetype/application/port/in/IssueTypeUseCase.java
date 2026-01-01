package com.tissue.issuetype.application.port.in;

import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.issuetype.application.dto.response.IssueTypeResponse;

public interface IssueTypeUseCase {

    IssueTypeResponse create(CreateIssueTypeCommand cmd);

    void rename(RenameIssueTypeCommand cmd);

    void update(PatchIssueTypeCommand cmd);

    void delete(DeleteIssueTypeCommand cmd);
}
