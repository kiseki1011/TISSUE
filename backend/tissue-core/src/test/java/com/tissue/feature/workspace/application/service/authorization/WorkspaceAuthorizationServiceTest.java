package com.tissue.feature.workspace.application.service.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.InsufficientWorkspaceRoleException;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WorkspaceAuthorizationServiceTest {

    private final WorkspaceAuthorizationService sut = new WorkspaceAuthorizationService();

    @Nested
    @DisplayName("require workspace admin")
    class RequireWorkspaceAdmin {

        @Test
        @DisplayName("success: OWNER passes admin check")
        void successOwnerPassesAdminCheck() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.OWNER);

            // when & then
            assertThatCode(() -> sut.requireWorkspaceAdmin(actor)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: ADMIN passes admin check")
        void successAdminPassesAdminCheck() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.ADMIN);

            // when & then
            assertThatCode(() -> sut.requireWorkspaceAdmin(actor)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: MEMBER throws InsufficientWorkspaceRoleException for admin check")
        void failAdminCheck_If_Member() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> sut.requireWorkspaceAdmin(actor))
                    .isInstanceOf(InsufficientWorkspaceRoleException.class);
        }
    }

    @Nested
    @DisplayName("require workspace owner")
    class RequireWorkspaceOwner {

        @Test
        @DisplayName("success: OWNER passes owner check")
        void successOwnerPassesOwnerCheck() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.OWNER);

            // when & then
            assertThatCode(() -> sut.requireWorkspaceOwner(actor)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: ADMIN throws InsufficientWorkspaceRoleException for owner check")
        void failOwnerCheck_If_Admin() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.ADMIN);

            // when & then
            assertThatThrownBy(() -> sut.requireWorkspaceOwner(actor))
                    .isInstanceOf(InsufficientWorkspaceRoleException.class);
        }
    }

    @Nested
    @DisplayName("require workspace role grant permission")
    class RequireRoleGrantPermission {

        @Test
        @DisplayName("success: OWNER can grant ADMIN role to MEMBER")
        void successOwnerGrantsAdminToMember() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.OWNER);

            // when & then
            assertThatCode(() -> sut.requireRoleGrantPermission(actor, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: granting OWNER role throws ForbiddenException")
        void failGrantingOwnerRole() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);

            // when & then
            assertThatThrownBy(() -> sut.requireRoleGrantPermission(actor, WorkspaceRole.OWNER, WorkspaceRole.MEMBER))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("fail: MEMBER trying role grant throws InsufficientWorkspaceRoleException")
        void failRoleGrant_If_Member() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);

            // when & then
            assertThatThrownBy(() -> sut.requireRoleGrantPermission(actor, WorkspaceRole.MEMBER, WorkspaceRole.MEMBER))
                    .isInstanceOf(InsufficientWorkspaceRoleException.class);
        }

        @Test
        @DisplayName("fail: ADMIN changing ADMIN role throws ForbiddenException")
        void failAdminChangingAdminRole() {
            // given
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.ADMIN);

            // when & then
            assertThatThrownBy(() -> sut.requireRoleGrantPermission(actor, WorkspaceRole.MEMBER, WorkspaceRole.ADMIN))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("require workspace invite link edit permission")
    class RequireInviteLinkEditPermission {

        @Test
        @DisplayName("success: ADMIN can edit any invite link")
        void successAdminCanEditAnyLink() {
            // given
            WorkspaceInviteLink inviteLink = mock(WorkspaceInviteLink.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            given(actor.getRole()).willReturn(WorkspaceRole.ADMIN);

            // when & then
            assertThatCode(() -> sut.requireInviteLinkEditPermission(inviteLink, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: link creator can edit link")
        void successLinkEdit_If_Creator() {
            // given
            WorkspaceInviteLink inviteLink = mock(WorkspaceInviteLink.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Member member = mock(Member.class);

            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);
            given(actor.getMember()).willReturn(member);
            given(member.getId()).willReturn(42L);
            given(inviteLink.getCreatedBy()).willReturn(42L);

            // when & then
            assertThatCode(() -> sut.requireInviteLinkEditPermission(inviteLink, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: link edit throws ForbiddenException if not creator (and MEMBER)")
        void failLinkEdit_If_NotCreatorAndMember() {
            // given
            WorkspaceInviteLink inviteLink = mock(WorkspaceInviteLink.class);
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Member member = mock(Member.class);

            given(actor.getRole()).willReturn(WorkspaceRole.MEMBER);
            given(actor.getMember()).willReturn(member);
            given(member.getId()).willReturn(42L);
            given(inviteLink.getCreatedBy()).willReturn(99L);

            // when & then
            assertThatThrownBy(() -> sut.requireInviteLinkEditPermission(inviteLink, actor))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
