package com.tissue.feature.wiki.application.service.authorization;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.DOCUMENT_DELETE_NOT_ALLOWED;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.DOCUMENT_LOCK_NOT_ALLOWED;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiAuthorizationService {

    public void requireDocumentLockPermission(WikiDocument document, Member actor) {
        if (actor.hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        if (isDocumentCreator(document, actor.getId())) {
            return;
        }
        throw new ForbiddenException(DOCUMENT_LOCK_NOT_ALLOWED);
    }

    public void requireDocumentDeletePermission(WikiDocument document, Member actor) {
        if (actor.hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        if (isDocumentCreator(document, actor.getId())) {
            return;
        }
        throw new ForbiddenException(DOCUMENT_DELETE_NOT_ALLOWED);
    }

    private boolean isDocumentCreator(WikiDocument document, Long actorMemberId) {
        return document.getCreatedBy().equals(actorMemberId);
    }
}
