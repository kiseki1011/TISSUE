package com.tissue.feature.organization.team.application.service.validator;

import static com.tissue.feature.organization.team.domain.exception.TeamErrorCode.DUPLICATE_TEAM_NAME;

import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamValidator {

    private final TeamRepository teamRepository;

    public void ensureUniqueLabel(Name name) {
        boolean duplicated = teamRepository.existsByName_NormalizedName(name.getNormalizedName());
        if (duplicated) {
            throw new ResourceConflictException(DUPLICATE_TEAM_NAME);
        }
    }
}
