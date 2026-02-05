package com.tissue.issuetype.application.port.in;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;

public interface IssueFieldUseCase {

    IssueFieldResponse create(Long issueTypeId, CreateIssueFieldCommand cmd, ProjectMemberContext actorContext);

    void rename(Long issueTypeId, Long issueFieldId, Name name, ProjectMemberContext actorContext);

    void update(Long issueTypeId, Long issueFieldId, PatchIssueFieldCommand cmd, ProjectMemberContext actorContext);

    void delete(Long issueTypeId, Long issueFieldId, ProjectMemberContext actorContext);

    // TODO: Make and use IssueFieldOptionResponse
    IssueFieldResponse addOption(Long issueTypeId, Long issueFieldId, Name name, ProjectMemberContext actorContext);

    void renameOption(Long issueTypeId, Long issueFieldId, Long optionId, Name name, ProjectMemberContext actorContext);

    ReorderedOptionsResponse reorderOptions(
            Long issueTypeId, Long issueFieldId, List<Long> targetOrderedIds, ProjectMemberContext actorContext);

    void deleteOption(Long issueTypeId, Long issueFieldId, Long optionId, ProjectMemberContext actorContext);
}
