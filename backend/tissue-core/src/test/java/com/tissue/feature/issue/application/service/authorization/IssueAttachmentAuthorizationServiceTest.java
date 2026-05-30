package com.tissue.feature.issue.application.service.authorization;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ATTACHMENT_DELETE_NOT_ALLOWED;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.domain.IssueAttachment;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueAttachmentAuthorizationServiceTest {

    private final IssueAttachmentAuthorizationService sut = new IssueAttachmentAuthorizationService();

    @Nested
    @DisplayName("requireDeletePermission")
    class RequireDeletePermission {

        @Test
        @DisplayName("success: system admin can delete any attachment")
        void successAdminCanDeleteAttachment() {
            // given
            IssueAttachment attachment = mock(IssueAttachment.class);
            ProjectMember actor = mockProjectMember(SystemRole.ADMIN, 1L);

            // when & then
            assertThatNoException().isThrownBy(() -> sut.requireDeletePermission(attachment, actor));
        }

        @Test
        @DisplayName("success: uploader can delete own attachment")
        void successUploaderCanDeleteAttachment() {
            // given
            IssueAttachment attachment = mock(IssueAttachment.class);
            given(attachment.isUploader(1L)).willReturn(true);
            ProjectMember actor = mockProjectMember(SystemRole.USER, 1L);

            // when & then
            assertThatNoException().isThrownBy(() -> sut.requireDeletePermission(attachment, actor));
        }

        @Test
        @DisplayName("fail: if project member is not uploader, cannot delete attachment")
        void failNonUploaderCannotDeleteAttachment() {
            // given
            IssueAttachment attachment = mock(IssueAttachment.class);
            given(attachment.isUploader(2L)).willReturn(false);
            ProjectMember actor = mockProjectMember(SystemRole.USER, 2L);

            // when & then
            assertThatThrownBy(() -> sut.requireDeletePermission(attachment, actor))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ATTACHMENT_DELETE_NOT_ALLOWED);
        }
    }

    private ProjectMember mockProjectMember(SystemRole role, Long memberId) {
        Member member = mock(Member.class);
        given(member.hasAtLeast(SystemRole.ADMIN)).willReturn(role.isEqualOrHigherThan(SystemRole.ADMIN));

        ProjectMember projectMember = mock(ProjectMember.class);
        given(projectMember.getMember()).willReturn(member);
        given(projectMember.getMemberId()).willReturn(memberId);
        return projectMember;
    }
}
