package com.tissue.issuetype.application.service.finder;

import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.EnumFieldOptionNotFoundException;
import com.tissue.issuetype.domain.exception.IssueFieldNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

    private final IssueFieldQueryRepository issueFieldQueryRepository;
    private final EnumFieldOptionQueryRepository optionQueryRepository;

    public IssueField getBy(Long issueFieldId, IssueType issueType) {
        return issueFieldQueryRepository
                .findByIdAndIssueType(issueFieldId, issueType)
                .orElseThrow(() -> new IssueFieldNotFoundException(issueFieldId, issueType));
    }

    public List<IssueField> getAllByIssueType(IssueType issueType) {
        return issueFieldQueryRepository.findByIssueType(issueType);
    }

    public EnumFieldOption getOptionBy(Long optionId, IssueField field) {
        return optionQueryRepository
                .findByIdAndIssueField(optionId, field)
                .orElseThrow(() -> new EnumFieldOptionNotFoundException(optionId, field));
    }

    public List<EnumFieldOption> getAllOptions(IssueField field) {
        return optionQueryRepository.findByIssueFieldOrderByPositionAsc(field);
    }

    public int countOptions(IssueField issueField) {
        return optionQueryRepository.countByIssueField(issueField);
    }
}
