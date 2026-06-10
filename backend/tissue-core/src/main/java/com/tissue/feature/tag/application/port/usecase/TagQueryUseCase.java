package com.tissue.feature.tag.application.port.usecase;

import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.shared.dto.ProjectIdentifier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TagQueryUseCase {

    Page<TagDetail> searchTags(ProjectIdentifier pid, @Nullable String keyword, Pageable pageable, Long actorMemberId);
}
