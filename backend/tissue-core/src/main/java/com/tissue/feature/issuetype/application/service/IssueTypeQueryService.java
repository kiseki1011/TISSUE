package com.tissue.feature.issuetype.application.service;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeQueryUseCase;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.service.MemberFinder;
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
    private final MemberFinder memberFinder;

    @Override
    public List<IssueTypeSummary> getIssueTypes(Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        List<IssueType> issueTypes = issueTypeRepository.findAllWithWorkflow();

        return issueTypes.stream().map(IssueTypeSummary::from).toList();
    }

    @Override
    public IssueTypeDetail getIssueTypeDetail(Long issueTypeId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        IssueType issueType = issueTypeFinder.getById(issueTypeId);

        List<IssueField> fields = issueFieldRepository.findAllWithOptionsByIssueTypeId(issueType.getId());

        return IssueTypeDetail.of(issueType, fields);
    }
}
