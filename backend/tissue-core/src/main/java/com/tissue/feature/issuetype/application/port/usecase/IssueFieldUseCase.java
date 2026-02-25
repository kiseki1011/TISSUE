package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;

public interface IssueFieldUseCase {
    // spotless:off
    IssueFieldResponse create(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        CreateIssueFieldCommand cmd,
        Long actorMemberId);

    void rename(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        Long issueFieldId,
        Name name,
        Long actorMemberId);

    void update(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        Long issueFieldId,
        PatchIssueFieldCommand cmd,
        Long actorMemberId);

    void delete(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        Long issueFieldId,
        Long actorMemberId);

    IssueFieldResponse addOption(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        Long issueFieldId,
        Name name,
        Long actorMemberId);

    void renameOption(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        Long issueFieldId,
        Long optionId,
        Name name,
        Long actorMemberId);

    void deleteOption(
        ProjectIdentifier projectIdentifier,
        Long issueTypeId,
        Long issueFieldId,
        Long optionId,
        Long actorMemberId);
    // spotless:on
}
