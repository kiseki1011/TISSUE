package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.shared.vo.Name;
import java.util.List;

public interface IssueFieldUseCase {

    IssueFieldResponse create(
            String projectKey, Long issueTypeId, CreateIssueFieldCommand cmd, WorkspaceMemberContext actorContext);

    void rename(String projectKey, Long issueTypeId, Long issueFieldId, Name name, WorkspaceMemberContext actorContext);

    void update(
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            PatchIssueFieldCommand cmd,
            WorkspaceMemberContext actorContext);

    void delete(String projectKey, Long issueTypeId, Long issueFieldId, WorkspaceMemberContext actorContext);

    // TODO: Make and use IssueFieldOptionResponse?
    IssueFieldResponse addOption(
            String projectKey, Long issueTypeId, Long issueFieldId, Name name, WorkspaceMemberContext actorContext);

    void renameOption(
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            Long optionId,
            Name name,
            WorkspaceMemberContext actorContext);

    ReorderedOptionsResponse reorderOptions(
            String projectKey,
            Long issueTypeId,
            Long issueFieldId,
            List<Long> targetOrderedIds,
            WorkspaceMemberContext actorContext);

    void deleteOption(
            String projectKey, Long issueTypeId, Long issueFieldId, Long optionId, WorkspaceMemberContext actorContext);
}
