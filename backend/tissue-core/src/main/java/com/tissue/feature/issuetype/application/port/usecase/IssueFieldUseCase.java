package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import java.util.List;

public interface IssueFieldUseCase {

    IssueFieldResponse create(
            ProjectIdentifier projectIdentifier, Long issueTypeId, CreateIssueFieldCommand cmd, Long memberId);

    void rename(ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Name name, Long memberId);

    void update(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            PatchIssueFieldCommand cmd,
            Long memberId);

    void delete(ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Long memberId);

    IssueFieldResponse addOption(
            ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Name name, Long memberId);

    void renameOption(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            Long optionId,
            Name name,
            Long memberId);

    ReorderedOptionsResponse reorderOptions(
            ProjectIdentifier projectIdentifier,
            Long issueTypeId,
            Long issueFieldId,
            List<Long> targetOrderedIds,
            Long memberId);

    void deleteOption(
            ProjectIdentifier projectIdentifier, Long issueTypeId, Long issueFieldId, Long optionId, Long memberId);
}
