package com.tissue.feature.wiki.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.wiki.domain.exception.WikiLinkTargetNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiLinkTargetResolver {

    private final IssueQueryRepository issueQueryRepository;
    private final ProjectQueryRepository projectQueryRepository;
    private final WikiDocumentQueryRepository wikiDocumentQueryRepository;

    /**
     * Validates that the link target exists.
     *
     * <p>Wiki is a single global space, so a link may point at any issue, project, or document in
     * the deployment.
     */
    public void ensureTargetExists(WikiLinkTargetType targetType, Long targetId) {
        boolean exists =
                switch (targetType) {
                    case ISSUE -> issueQueryRepository.findById(targetId).isPresent();
                    case PROJECT -> projectQueryRepository.findById(targetId).isPresent();
                    case WIKI_DOC ->
                        wikiDocumentQueryRepository.findById(targetId).isPresent();
                };
        if (!exists) {
            throw new WikiLinkTargetNotFoundException(targetType, targetId);
        }
    }
}
