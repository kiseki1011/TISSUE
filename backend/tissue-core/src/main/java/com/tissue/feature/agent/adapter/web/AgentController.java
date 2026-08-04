package com.tissue.feature.agent.adapter.web;

import com.tissue.feature.agent.adapter.web.request.CreateAgentRequest;
import com.tissue.feature.agent.adapter.web.request.UpdateAgentRequest;
import com.tissue.feature.agent.application.dto.AgentResponse;
import com.tissue.feature.agent.application.port.usecase.AgentUseCase;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agents")
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentUseCase agentUseCase;

    @Operation(operationId = "createAgent", summary = "Create an agent", description = """
                    Register a new agent owned by the current member. The agent is treated as a member
                    that acts at USER level and authenticates with its own Personal Access Token.
                    The name must be unique among your agents.""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Agent created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Duplicate agent name", content = @Content)
    })
    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(
            @RequestBody @Valid CreateAgentRequest request, @CurrentMember MemberDetails memberDetails) {
        AgentResponse response = agentUseCase.createAgent(memberDetails.getMemberId(), request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateAgent", summary = "Update an agent", description = """
                    Update one of your agents' type, model, or description. Only provided fields are
                    updated.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Agent updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @PatchMapping("/{agentId}")
    public ResponseEntity<Void> updateAgent(
            @PathVariable Long agentId,
            @RequestBody @Valid UpdateAgentRequest request,
            @CurrentMember MemberDetails memberDetails) {
        agentUseCase.updateAgent(memberDetails.getMemberId(), agentId, request.toCommand());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "listMyAgents",
            summary = "List my agents",
            description = "List the active agents owned by the current member.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Agents retrieved"))
    @GetMapping
    public ResponseEntity<List<AgentResponse>> listMyAgents(@CurrentMember MemberDetails memberDetails) {
        return ResponseEntity.ok(agentUseCase.listAgents(memberDetails.getMemberId()));
    }

    @Operation(
            operationId = "deactivateAgent",
            summary = "Deactivate an agent",
            description = "Deactivate one of your agents. Its tokens stop working immediately.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Agent deactivated"),
        @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content)
    })
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> deactivateAgent(
            @PathVariable Long agentId, @CurrentMember MemberDetails memberDetails) {
        agentUseCase.deactivateAgent(memberDetails.getMemberId(), agentId);

        return ResponseEntity.noContent().build();
    }
}
