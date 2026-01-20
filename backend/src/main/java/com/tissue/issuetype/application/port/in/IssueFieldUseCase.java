package com.tissue.issuetype.application.port.in;

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

public interface IssueFieldUseCase {

    IssueFieldResponse create(CreateIssueFieldCommand cmd);

    void rename(RenameIssueFieldCommand cmd);

    void update(PatchIssueFieldCommand cmd);

    void delete(DeleteIssueFieldCommand cmd);

    // TODO: IssueFieldOptionResponse를 만들어서 사용하는걸 고려
    IssueFieldResponse addOption(AddOptionCommand cmd);

    void renameOption(RenameOptionCommand cmd);

    ReorderedOptionsResponse reorderOptions(ReorderOptionsCommand cmd);

    void deleteOption(DeleteOptionCommand cmd);
}
