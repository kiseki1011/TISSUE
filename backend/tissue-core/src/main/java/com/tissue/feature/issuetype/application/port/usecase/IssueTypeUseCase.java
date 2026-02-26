package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import java.util.List;

public interface IssueTypeUseCase {

    IssueTypeResponse create(ProjectIdentifier projectIdentifier, CreateIssueTypeCommand cmd, Long actorMemberId);

    void rename(ProjectIdentifier projectIdentifier, Long issueTypeId, Name name, Long actorMemberId);

    void update(ProjectIdentifier projectIdentifier, Long issueTypeId, PatchIssueTypeCommand cmd, Long actorMemberId);

    void delete(ProjectIdentifier projectIdentifier, Long issueTypeId, Long actorMemberId);

    void reorderFields(
            ProjectIdentifier projectIdentifier, Long issueTypeId, List<Long> orderedIds, Long actorMemberId);
}
