package com.tissue.feature.wiki.application.port.usecase;

import com.tissue.feature.wiki.application.dto.response.WikiTagDetail;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WikiTagQueryUseCase {

    Page<WikiTagDetail> searchTags(@Nullable String keyword, Pageable pageable, Long actorMemberId);
}
