package com.tissue.feature.issuetype.application.port.usecase;

import com.tissue.feature.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.request.PatchIssueTypeCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import java.util.List;

public interface IssueTypeUseCase {

    IssueTypeResponse create(ProjectIdentifier pid, CreateIssueTypeCommand cmd, Long actorMemberId);

    void rename(ProjectIdentifier pid, Long issueTypeId, Name name, Long actorMemberId);

    void update(ProjectIdentifier pid, Long issueTypeId, PatchIssueTypeCommand cmd, Long actorMemberId);

    void delete(ProjectIdentifier pid, Long issueTypeId, Long actorMemberId);

    void reorderFields(ProjectIdentifier pid, Long issueTypeId, List<Long> orderedIds, Long actorMemberId);
}
