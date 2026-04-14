package com.tissue.feature.wiki.application.service.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WikiAuthorizationServiceTest {

    private final WikiAuthorizationService sut = new WikiAuthorizationService();

    @Nested
    @DisplayName("require document lock permission")
    class RequireDocumentLockPermission {

        @Test
        @DisplayName("success: OWNER passes lock permission check")
        void successOwnerPassesLockCheck() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.OWNER);

            // when & then
            assertThatCode(() -> sut.requireDocumentLockPermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: ADMIN passes lock permission check")
        void successAdminPassesLockCheck() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.ADMIN);

            // when & then
            assertThatCode(() -> sut.requireDocumentLockPermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: document creator passes lock permission check")
        void successCreatorPassesLockCheck() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Member member = mock(Member.class);

            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);
            given(actor.getMember()).willReturn(member);
            given(member.getId()).willReturn(1L);
            given(document.getCreatedBy()).willReturn(1L);

            // when & then
            assertThatCode(() -> sut.requireDocumentLockPermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: MEMBER that is not creator throws ForbiddenException for lock check")
        void failLockCheck_If_NotCreatorAndMember() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Member member = mock(Member.class);

            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);
            given(actor.getMember()).willReturn(member);
            given(member.getId()).willReturn(1L);
            given(document.getCreatedBy()).willReturn(2L);

            // when & then
            assertThatThrownBy(() -> sut.requireDocumentLockPermission(document, actor))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("require document delete permission")
    class RequireDocumentDeletePermission {

        @Test
        @DisplayName("success: ADMIN passes delete permission check")
        void successAdminPassesDeleteCheck() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.ADMIN);

            // when & then
            assertThatCode(() -> sut.requireDocumentDeletePermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: OWNER passes delete permission check")
        void successOwnerPassesDeleteCheck() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.OWNER);

            // when & then
            assertThatCode(() -> sut.requireDocumentDeletePermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: document author passes delete permission check")
        void successCreatorPassesDeleteCheck() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Member member = mock(Member.class);

            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);
            given(actor.getMember()).willReturn(member);
            given(member.getId()).willReturn(1L);
            given(document.getCreatedBy()).willReturn(1L);

            // when & then
            assertThatCode(() -> sut.requireDocumentDeletePermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: MEMBER that is not author throws ForbiddenException for delete check")
        void failDeleteCheck_If_NotCreatorAndMember() {
            // given
            WikiDocument document = mock(WikiDocument.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Member member = mock(Member.class);

            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);
            given(actor.getMember()).willReturn(member);
            given(member.getId()).willReturn(1L);
            given(document.getCreatedBy()).willReturn(2L);

            // when & then
            assertThatThrownBy(() -> sut.requireDocumentDeletePermission(document, actor))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
