package com.tissue.feature.wiki.application.service.finder;

import com.tissue.feature.wiki.application.port.repository.WikiTagRepository;
import com.tissue.feature.wiki.domain.WikiTag;
import com.tissue.feature.wiki.domain.exception.WikiTagNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiTagFinder {

    private final WikiTagRepository wikiTagRepository;

    public WikiTag getById(Long wikiTagId) {
        return wikiTagRepository.findById(wikiTagId).orElseThrow(() -> new WikiTagNotFoundException(wikiTagId));
    }
}
