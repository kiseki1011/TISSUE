package com.tissue.feature.tag.application.service;

import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.feature.tag.domain.exception.TagNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagFinder {

    private final TagRepository tagRepository;

    public Tag getWithProjectBy(String workspaceKey, String projectKey, Long tagId) {
        return tagRepository
                .findByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, tagId)
                .orElseThrow(() -> new TagNotFoundException(projectKey, tagId));
    }

    public Tag getWithProject(String workspaceKey, Long tagId) {
        return tagRepository
                .findByWorkspaceKeyAndId(workspaceKey, tagId)
                .orElseThrow(() -> new TagNotFoundException(tagId));
    }

    public List<Tag> getAllBy(String workspaceKey, String projectKey) {
        return tagRepository.findAllByWorkspaceKeyAndProjectKey(workspaceKey, projectKey);
    }
}
