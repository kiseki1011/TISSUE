package com.tissue.feature.agent.model.adapter.web;

import com.tissue.feature.agent.model.adapter.web.request.CreateAiModelRequest;
import com.tissue.feature.agent.model.adapter.web.request.UpdateAiModelRequest;
import com.tissue.feature.agent.model.application.dto.response.AiModelResponse;
import com.tissue.feature.agent.model.application.port.usecase.AiModelUseCase;
import com.tissue.feature.agent.model.domain.exception.AiModelErrorCode;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.AiModelErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.auth.RequireSystemAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent Model")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiModelController {

    private final AiModelUseCase aiModelUseCase;

    @Operation(operationId = "createAiModel", summary = "Create AI model", description = """
                Create a new global AI model catalog entry (ex: "claude-opus-4-8").

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "AI model created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @AiModelErrors({AiModelErrorCode.DUPLICATE_AI_MODEL_NAME})
    @RequireSystemAdmin
    @PostMapping("/models")
    public ResponseEntity<AiModelResponse> createAiModel(
            @RequestBody @Valid CreateAiModelRequest req, @CurrentMember MemberDetails memberDetails) {
        AiModelResponse response = aiModelUseCase.create(req.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateAiModel", summary = "Update AI model", description = """
                Update an AI model's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "AI model updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @AiModelErrors({
        AiModelErrorCode.AI_MODEL_NOT_FOUND,
        AiModelErrorCode.DUPLICATE_AI_MODEL_NAME,
    })
    @RequireSystemAdmin
    @PatchMapping("/models/{modelId}")
    public ResponseEntity<Void> updateAiModel(
            @PathVariable Long modelId,
            @RequestBody @Valid UpdateAiModelRequest request,
            @CurrentMember MemberDetails memberDetails) {
        aiModelUseCase.update(modelId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteAiModel", summary = "Delete AI model", description = """
                Permanently delete a global AI model. Any agent currently assigned to it is unassigned.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "AI model deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @AiModelErrors({AiModelErrorCode.AI_MODEL_NOT_FOUND})
    @RequireSystemAdmin
    @DeleteMapping("/models/{modelId}")
    public ResponseEntity<Void> deleteAiModel(@PathVariable Long modelId, @CurrentMember MemberDetails memberDetails) {
        aiModelUseCase.delete(modelId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
