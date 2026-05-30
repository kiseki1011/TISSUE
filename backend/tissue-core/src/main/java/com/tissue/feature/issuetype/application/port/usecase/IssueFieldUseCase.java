package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.shared.vo.Name;

public interface IssueFieldUseCase {
    // spotless:off
    IssueFieldResponse addField(
        Long issueTypeId,
        CreateIssueFieldCommand cmd,
        Long actorMemberId);

    void update(
        Long issueFieldId,
        PatchIssueFieldCommand cmd,
        Long actorMemberId);

    void delete(
        Long issueFieldId,
        Long actorMemberId);

    IssueFieldResponse addOption(
        Long issueFieldId,
        Name name,
        Long actorMemberId);

    void updateOption(
        Long issueFieldId,
        Long optionId,
        Name name,
        Long actorMemberId);

    void deleteOption(
        Long issueFieldId,
        Long optionId,
        Long actorMemberId);
    // spotless:on
}
