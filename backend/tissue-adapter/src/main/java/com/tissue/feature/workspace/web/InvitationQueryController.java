package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.feature.workspace.application.port.usecase.InvitationQueryUseCase;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invitation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invitations")
public class InvitationQueryController {

    private final InvitationQueryUseCase invitationQueryUseCase;

    @Operation(operationId = "listMyInvitations", summary = "List my invitations", description = """
                    List all pending invitations for the current user.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Invitations retrieved")})
    @GetMapping
    public ResponseEntity<List<InvitationDetail>> listMyInvitations(@CurrentMember MemberDetails memberDetails) {
        List<InvitationDetail> response = invitationQueryUseCase.getMyInvitations(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
