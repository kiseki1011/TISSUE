package com.tissue.admin.adapter.web;

import com.tissue.admin.adapter.web.request.ChangeSystemRoleRequest;
import com.tissue.admin.application.dto.AdminMemberDetail;
import com.tissue.admin.application.dto.AdminMemberSummary;
import com.tissue.admin.application.port.usecase.AdminMemberUseCase;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.LocalAuthOnly;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.auth.RequireSuperAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Global Member Management")
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@RequireSuperAdmin
public class AdminMemberController {

    private final AdminMemberUseCase adminMemberUseCase;

    @Operation(operationId = "adminListMembers", summary = "List/search members", description = """
                List all members instance-wide, with optional `status`, `role`, and `keyword`
                (matches username/name/email) filters. Includes deleted/purged members.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Members retrieved")})
    @GetMapping
    public ResponseEntity<Page<AdminMemberSummary>> listMembers(
            @RequestParam(required = false) @Nullable MemberStatus status,
            @RequestParam(required = false) @Nullable SystemRole role,
            @RequestParam(required = false) @Nullable String keyword,
            Pageable pageable) {
        Page<AdminMemberSummary> response = adminMemberUseCase.listMembers(status, role, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "adminGetMember", summary = "Get member detail", description = """
                Get full detail for a single member, in any status.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member retrieved"),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND})
    @GetMapping("/{memberId}")
    public ResponseEntity<AdminMemberDetail> getMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(adminMemberUseCase.getMember(memberId));
    }

    @Operation(operationId = "adminChangeSystemRole", summary = "Change a member's system role", description = """
                Promote/demote a member's system role (SUPER_ADMIN/ADMIN/USER). The last active
                SUPER_ADMIN cannot be demoted, and a SUPER_ADMIN cannot demote themselves.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role changed"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Last super admin / self-demotion", content = @Content)
    })
    @MemberErrors({
        MemberErrorCode.SUPER_ADMIN_REQUIRED,
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
        MemberErrorCode.LAST_SUPER_ADMIN,
        MemberErrorCode.CANNOT_DEMOTE_SELF_SUPER_ADMIN,
    })
    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> changeSystemRole(
            @PathVariable Long memberId,
            @RequestBody @Valid ChangeSystemRoleRequest request,
            @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.changeSystemRole(memberId, request.role(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "adminForceWithdraw", summary = "Force-withdraw a member", description = """
                Withdraw (soft-delete) a member's account on their behalf and revoke their sessions.
                The last active SUPER_ADMIN cannot be withdrawn.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member withdrawn"),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Last super admin", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_DELETED, MemberErrorCode.LAST_SUPER_ADMIN})
    @PostMapping("/{memberId}/withdraw")
    public ResponseEntity<Void> forceWithdraw(@PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.forceWithdraw(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "adminForceRestore", summary = "Restore a withdrawn member", description = """
                Restore a member whose account is in the `DELETED` state back to `ACTIVE`.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member restored"),
        @ApiResponse(responseCode = "400", description = "Member is not deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_NOT_DELETED})
    @PostMapping("/{memberId}/restore")
    public ResponseEntity<Void> forceRestore(@PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.forceRestore(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "adminRevokeSessions", summary = "Revoke a member's sessions", description = """
                Force-logout a member by deleting their refresh token. NOTE: an already-issued access
                token remains valid until it expires (no token blacklist); this blocks the next refresh.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sessions revoked"),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND})
    @DeleteMapping("/{memberId}/sessions")
    public ResponseEntity<Void> revokeSessions(
            @PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.revokeSessions(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "adminLockMember", summary = "Lock a member account", description = """
                Lock a member out: they can no longer log in or refresh tokens (existing access tokens remain valid
                until they expire). Their sessions are revoked. A SUPER_ADMIN cannot be locked.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member locked"),
        @ApiResponse(responseCode = "400", description = "Member is not active", content = @Content),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Already locked / cannot lock super admin", content = @Content)
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_NOT_ACTIVE,
        MemberErrorCode.MEMBER_ALREADY_LOCKED,
        MemberErrorCode.CANNOT_LOCK_SUPER_ADMIN
    })
    @PostMapping("/{memberId}/lock")
    public ResponseEntity<Void> lockMember(@PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.lockMember(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "adminUnlockMember", summary = "Unlock a member account", description = """
                Restore a locked member back to active so they can log in again.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member unlocked"),
        @ApiResponse(responseCode = "400", description = "Member is not locked", content = @Content),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_NOT_LOCKED})
    @PostMapping("/{memberId}/unlock")
    public ResponseEntity<Void> unlockMember(@PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.unlockMember(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "adminPurgeMember", summary = "Permanently purge a deleted member", description = """
                Irreversibly wipe a `DELETED` member's PII (email/username/name) and their credentials, transitioning
                them to `PURGED`. The row is kept as an attribution anchor for issues/comments. This is the manual
                equivalent of the retention sweep; the member must already be `DELETED`.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member purged"),
        @ApiResponse(responseCode = "400", description = "Member is not deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_NOT_DELETED})
    @PostMapping("/{memberId}/purge")
    public ResponseEntity<Void> purgeMember(@PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.purgeMember(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "adminForcePasswordReset",
            summary = "Send a member a password-reset email",
            description = """
                Trigger the standard password-reset email flow for an active member (the member completes the reset
                themselves via the link). The admin never sees or sets the password. The member must be active and
                have an email address.

                Available only in `LOCAL` auth mode — in `OIDC` mode passwords are managed by the \
                identity provider, so this endpoint is disabled (returns 403).

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password-reset email triggered"),
        @ApiResponse(responseCode = "400", description = "Member not active / has no email", content = @Content),
        @ApiResponse(responseCode = "403", description = "Disabled in OIDC auth mode", content = @Content),
        @ApiResponse(responseCode = "404", description = "Member not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.MEMBER_NOT_FOUND, MemberErrorCode.MEMBER_NOT_ACTIVE, MemberErrorCode.MEMBER_NO_EMAIL
    })
    @LocalAuthOnly
    @PostMapping("/{memberId}/reset-password")
    public ResponseEntity<Void> forcePasswordReset(
            @PathVariable Long memberId, @CurrentMember MemberDetails memberDetails) {
        adminMemberUseCase.forcePasswordReset(memberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
