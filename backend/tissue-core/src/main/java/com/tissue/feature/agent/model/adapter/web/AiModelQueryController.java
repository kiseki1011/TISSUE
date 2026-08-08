package com.tissue.feature.agent.model.adapter.web;

import com.tissue.feature.agent.model.application.dto.response.AiModelSummary;
import com.tissue.feature.agent.model.application.port.usecase.AiModelQueryUseCase;
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

@Tag(name = "Agent Model")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiModelQueryController {

    private final AiModelQueryUseCase aiModelQueryUseCase;

    @Operation(operationId = "listAiModels", summary = "List AI models", description = """
                    List all global AI models.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "AI models retrieved")})
    @GetMapping("/models")
    public ResponseEntity<List<AiModelSummary>> listAiModels(@CurrentMember MemberDetails memberDetails) {
        List<AiModelSummary> response = aiModelQueryUseCase.getModels(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
