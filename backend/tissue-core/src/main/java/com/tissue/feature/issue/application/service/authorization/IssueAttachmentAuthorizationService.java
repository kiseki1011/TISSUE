package com.tissue.feature.issue.application.service.authorization;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_DELETE_NOT_ALLOWED;

import com.tissue.feature.issue.domain.IssueAttachment;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueAttachmentAuthorizationService {

    public void requireDeletePermission(IssueAttachment attachment, ProjectMember actor) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (actor.isManager()) {
            return;
        }
        if (attachment.isUploader(actor.getMemberId())) {
            return;
        }
        throw new ForbiddenException(ATTACHMENT_DELETE_NOT_ALLOWED);
    }
}
