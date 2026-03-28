package com.tissue.feature.attachment.application.service;

import static com.tissue.feature.attachment.domain.exception.AttachmentErrorCode.ATTACHMENT_DELETE_NOT_ALLOWED;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.attachment.domain.IssueAttachment;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AttachmentAuthorizationServiceTest {

    private final AttachmentAuthorizationService sut = new AttachmentAuthorizationService();

    @Nested
    @DisplayName("requireDeletePermission")
    class RequireDeletePermission {

        @Test
        @DisplayName("success: admin can delete any attachment")
        void successAdminCanDeleteAttachment() {
            // given
            IssueAttachment attachment = mock(IssueAttachment.class);
            ProjectMember actor = mockProjectMember(WorkspaceRole.ADMIN, 1L);

            // when & then
            assertThatNoException().isThrownBy(() -> sut.requireDeletePermission(attachment, actor));
        }

        @Test
        @DisplayName("success: uploader can delete own attachment")
        void successUploaderCanDeleteAttachment() {
            // given
            IssueAttachment attachment = mock(IssueAttachment.class);
            given(attachment.isUploader(1L)).willReturn(true);
            ProjectMember actor = mockProjectMember(WorkspaceRole.MEMBER, 1L);

            // when & then
            assertThatNoException().isThrownBy(() -> sut.requireDeletePermission(attachment, actor));
        }

        @Test
        @DisplayName("fail: if project member is not uploader, cannot delete attachment")
        void failNonUploaderCannotDeleteAttachment() {
            // given
            IssueAttachment attachment = mock(IssueAttachment.class);
            given(attachment.isUploader(2L)).willReturn(false);
            ProjectMember actor = mockProjectMember(WorkspaceRole.MEMBER, 2L);

            // when & then
            assertThatThrownBy(() -> sut.requireDeletePermission(attachment, actor))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_DELETE_NOT_ALLOWED);
        }
    }

    private ProjectMember mockProjectMember(WorkspaceRole role, Long memberId) {
        WorkspaceMember workspaceMember = mock(WorkspaceMember.class);
        given(workspaceMember.getRole()).willReturn(role);

        ProjectMember projectMember = mock(ProjectMember.class);
        given(projectMember.getWorkspaceMember()).willReturn(workspaceMember);
        given(projectMember.getMemberId()).willReturn(memberId);
        return projectMember;
    }
}
