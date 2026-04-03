package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.shared.vo.Name;

public interface IssueFieldUseCase {
    // spotless:off
    IssueFieldResponse addField(
        String workspaceKey,
        Long issueTypeId,
        CreateIssueFieldCommand cmd,
        Long actorMemberId);

    void update(
        String workspaceKey,
        Long issueFieldId,
        PatchIssueFieldCommand cmd,
        Long actorMemberId);

    void delete(
        String workspaceKey,
        Long issueFieldId,
        Long actorMemberId);

    IssueFieldResponse addOption(
        String workspaceKey,
        Long issueFieldId,
        Name name,
        Long actorMemberId);

    void updateOption(
        String workspaceKey,
        Long issueFieldId,
        Long optionId,
        Name name,
        Long actorMemberId);

    void deleteOption(
        String workspaceKey,
        Long issueFieldId,
        Long optionId,
        Long actorMemberId);
    // spotless:on
}
