package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.application.port.repository.IssueCustomFieldQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IssueCustomFieldQueryJpaAdapter implements IssueCustomFieldQueryPort {

    private final IssueCustomFieldJpaRepository jpaRepository;

    @Override
    public boolean existsWithCustomField(String fieldIdStr) {
        return jpaRepository.existsWithCustomField(fieldIdStr);
    }

    @Override
    public boolean isOptionInUse(String fieldIdStr, String optionIdStr) {
        return jpaRepository.isOptionInUse(fieldIdStr, optionIdStr);
    }
}
