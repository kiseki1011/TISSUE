package com.tissue.feature.tag.application.port.usecase;

import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.shared.dto.ProjectIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TagQueryUseCase {

    Page<TagDetail> getTagsByProject(ProjectIdentifier pid, Pageable pageable, Long actorMemberId);
}
