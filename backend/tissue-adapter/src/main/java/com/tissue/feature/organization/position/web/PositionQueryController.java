package com.tissue.feature.organization.position.web;

import com.tissue.feature.organization.position.application.dto.response.PositionSummary;
import com.tissue.feature.organization.position.application.port.usecase.PositionQueryUseCase;
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

@Tag(name = "Position")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PositionQueryController {

    private final PositionQueryUseCase positionQueryUseCase;

    @Operation(operationId = "listPositions", summary = "List positions", description = """
                    List all global positions.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Positions retrieved")})
    @GetMapping("/positions")
    public ResponseEntity<List<PositionSummary>> listPositions(@CurrentMember MemberDetails memberDetails) {
        List<PositionSummary> response = positionQueryUseCase.getPositions(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
