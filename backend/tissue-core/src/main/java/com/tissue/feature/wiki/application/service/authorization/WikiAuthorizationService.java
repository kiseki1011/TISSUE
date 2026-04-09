package com.tissue.feature.wiki.application.service.authorization;

import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.DOCUMENT_DELETE_NOT_ALLOWED;
import static com.tissue.feature.wiki.domain.exception.WikiErrorCode.DOCUMENT_LOCK_NOT_ALLOWED;

import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiAuthorizationService {

    public void requireDocumentLockPermission(WikiDocument document, WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isDocumentCreator(document, actor.getMember().getId())) {
            return;
        }
        throw new ForbiddenException(DOCUMENT_LOCK_NOT_ALLOWED);
    }

    public void requireDocumentDeletePermission(WikiDocument document, WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isDocumentCreator(document, actor.getMember().getId())) {
            return;
        }
        throw new ForbiddenException(DOCUMENT_DELETE_NOT_ALLOWED);
    }

    private boolean isDocumentCreator(WikiDocument document, Long actorMemberId) {
        return document.getCreatedBy().equals(actorMemberId);
    }
}
