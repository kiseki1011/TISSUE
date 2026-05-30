package com.tissue.feature.tag.application.service;

import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.feature.tag.domain.exception.TagNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagFinder {

    private final TagRepository tagRepository;

    public Tag getWithProjectBy(String projectKey, Long tagId) {
        return tagRepository
                .findByProjectKeyAndId(projectKey, tagId)
                .orElseThrow(() -> new TagNotFoundException(projectKey, tagId));
    }

    public Tag getWithProject(Long tagId) {
        return tagRepository.findById(tagId).orElseThrow(() -> new TagNotFoundException(tagId));
    }
}
