package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import java.util.List;

public interface IssueTypeUseCase {

    IssueTypeResponse create(CreateIssueTypeCommand cmd, Long actorMemberId);

    void update(Long issueTypeId, PatchIssueTypeCommand cmd, Long actorMemberId);

    void delete(Long issueTypeId, Long actorMemberId);

    void reorderFields(Long issueTypeId, List<Long> orderedIds, Long actorMemberId);
}
