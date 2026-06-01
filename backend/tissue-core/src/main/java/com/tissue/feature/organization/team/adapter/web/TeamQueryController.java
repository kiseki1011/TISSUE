package com.tissue.feature.organization.team.adapter.web;

import com.tissue.feature.organization.team.application.dto.response.TeamSummary;
import com.tissue.feature.organization.team.application.port.usecase.TeamQueryUseCase;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
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

@Tag(name = "Team")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamQueryController {

    private final TeamQueryUseCase teamQueryUseCase;

    @Operation(operationId = "listTeams", summary = "List teams", description = """
                    List all global teams.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Teams retrieved")})
    @GetMapping("/teams")
    public ResponseEntity<List<TeamSummary>> listTeams(@CurrentMember MemberDetails memberDetails) {
        List<TeamSummary> response = teamQueryUseCase.getTeams(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
