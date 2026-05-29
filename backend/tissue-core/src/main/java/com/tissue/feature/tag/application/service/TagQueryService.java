package com.tissue.feature.tag.application.service;

import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.application.port.usecase.TagQueryUseCase;
import com.tissue.shared.dto.ProjectIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagQueryService implements TagQueryUseCase {

    private final ProjectMemberFinder projectMemberFinder;
    private final TagRepository tagRepository;

    @Override
    public Page<TagDetail> getTagsByProject(ProjectIdentifier pid, Pageable pageable, Long actorMemberId) {
        projectMemberFinder.getByProjectKey(pid.projectKey(), actorMemberId);

        return tagRepository.findAllByProjectKey(pid.projectKey(), pageable).map(TagDetail::from);
    }
}
