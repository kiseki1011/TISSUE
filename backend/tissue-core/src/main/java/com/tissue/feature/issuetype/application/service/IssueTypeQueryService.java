package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeQueryUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueTypeQueryService implements IssueTypeQueryUseCase {

    private final IssueTypeFinder issueTypeFinder;
    private final IssueTypeRepository issueTypeRepository;
    private final IssueFieldRepository issueFieldRepository;
    private final ProjectMemberFinder projectMemberFinder;

    @Override
    public List<IssueTypeSummary> getProjectIssueTypes(ProjectIdentifier pid, Long actorMemberId) {
        projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);

        List<IssueType> issueTypes = issueTypeRepository.findAllWithProjectAndWorkflowByWorkspaceKeyAndProjectKey(
                pid.workspaceKey(), pid.projectKey());

        return issueTypes.stream().map(IssueTypeSummary::from).toList();
    }

    @Override
    public IssueTypeDetail getIssueTypeDetail(String workspaceKey, Long issueTypeId, Long actorMemberId) {
        IssueType issueType = issueTypeFinder.getWithProjectBy(workspaceKey, issueTypeId);
        projectMemberFinder.getBy(issueType.getProject(), actorMemberId);

        List<IssueField> fields = issueFieldRepository.findAllWithOptionsByIssueTypeId(issueType.getId());

        return IssueTypeDetail.of(issueType, fields);
    }
}
