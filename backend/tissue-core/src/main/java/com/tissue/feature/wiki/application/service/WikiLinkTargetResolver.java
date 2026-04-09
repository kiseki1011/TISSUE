package com.tissue.feature.wiki.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentRepository;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.wiki.domain.exception.WikiLinkTargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiLinkTargetResolver {

    private final IssueQueryRepository issueQueryRepository;
    private final ProjectQueryRepository projectQueryRepository;
    private final WikiDocumentRepository wikiDocumentRepository;

    public String resolveWorkspaceKey(WikiLinkTargetType targetType, Long targetId) {
        return switch (targetType) {
            case ISSUE ->
                issueQueryRepository
                        .findById(targetId)
                        .orElseThrow(() -> new WikiLinkTargetNotFoundException(targetType, targetId))
                        .getWorkspaceKey();
            case PROJECT ->
                projectQueryRepository
                        .findById(targetId)
                        .orElseThrow(() -> new WikiLinkTargetNotFoundException(targetType, targetId))
                        .getWorkspaceKey();
            case WIKI_DOC ->
                wikiDocumentRepository
                        .findById(targetId)
                        .orElseThrow(() -> new WikiLinkTargetNotFoundException(targetType, targetId))
                        .getWorkspaceKey();
        };
    }
}
