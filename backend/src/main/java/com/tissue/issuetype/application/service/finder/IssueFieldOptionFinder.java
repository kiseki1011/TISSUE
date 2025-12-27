package com.tissue.issuetype.application.service.finder;

import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldOptionFinder {

    private final EnumFieldOptionQueryRepository optionQueryRepo;

    public EnumFieldOption findByIdAndIssueField(Long optionId, IssueField field) {
        return optionQueryRepo
                .findByIdAndIssueField(optionId, field)
                .orElseThrow(() -> IssueTypeExceptions.optionNotFound(optionId, field));
    }

    public List<EnumFieldOption> findActiveOptions(IssueField field) {
        return optionQueryRepo.findByIssueFieldOrderByPositionAsc(field);
    }
}
