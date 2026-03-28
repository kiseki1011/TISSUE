package com.tissue.feature.attachment.application.service;

import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_DELETE_NOT_ALLOWED;

import com.tissue.feature.attachment.domain.IssueAttachment;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttachmentAuthorizationService {

    public void requireDeletePermission(IssueAttachment attachment, ProjectMember actor) {
        if (actor.getWorkspaceMember().getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (attachment.isUploader(actor.getMemberId())) {
            return;
        }
        throw new ForbiddenException(ATTACHMENT_DELETE_NOT_ALLOWED);
    }
}
