package com.tissue.feature.tag.application.service;

import static com.tissue.feature.tag.domain.exception.TagErrorCode.DUPLICATE_TAG_NAME;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagValidator {

    private final TagRepository tagRepository;

    public void ensureUniqueName(Project project, Name name) {
        if (tagRepository.existsByName_NormalizedAndProject(name.getNormalized(), project)) {
            throw new ResourceConflictException(DUPLICATE_TAG_NAME);
        }
    }

    public void ensureDeletable(Tag tag) {
        // TODO: IssueTag 구현 후 existsByTag 체크 추가
    }
}
