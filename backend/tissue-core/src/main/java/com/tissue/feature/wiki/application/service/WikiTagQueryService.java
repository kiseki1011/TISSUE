package com.tissue.feature.wiki.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.wiki.application.dto.response.WikiTagDetail;
import com.tissue.feature.wiki.application.port.repository.WikiTagRepository;
import com.tissue.feature.wiki.application.port.usecase.WikiTagQueryUseCase;
import com.tissue.support.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WikiTagQueryService implements WikiTagQueryUseCase {

    private final MemberFinder memberFinder;
    private final WikiTagRepository wikiTagRepository;

    @Override
    public Page<WikiTagDetail> searchTags(@Nullable String keyword, Pageable pageable, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        if (keyword == null || keyword.isBlank()) {
            return wikiTagRepository.findAll(pageable).map(WikiTagDetail::from);
        }

        String normalized = TextNormalizer.normalizeForUniq(keyword);
        return wikiTagRepository
                .findByName_NormalizedNameContaining(normalized, pageable)
                .map(WikiTagDetail::from);
    }
}
