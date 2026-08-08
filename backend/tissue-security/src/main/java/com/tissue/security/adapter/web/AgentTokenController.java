package com.tissue.security.adapter.web;

import com.tissue.feature.agent.application.service.AgentService;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.adapter.web.request.CreatePatRequest;
import com.tissue.security.application.dto.GeneratedToken;
import com.tissue.security.application.dto.response.CreatedPatResponse;
import com.tissue.security.application.dto.response.PatResponse;
import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent Tokens")
@RestController
@RequestMapping("/api/v1/agents/{agentId}/tokens")
@RequiredArgsConstructor
public class AgentTokenController {

    private final AgentService agentService;
    private final PersonalAccessTokenService personalAccessTokenService;

    @Operation(operationId = "issueAgentToken", summary = "Issue a token", description = """
                    Generate a Personal Access Token (PAT) for one of your agents. The secret is returned
                    exactly once and only its hash is stored. If lost, revoke it and issue a new one.""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Token issued"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Duplicate token name", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CreatedPatResponse> issueToken(
            @PathVariable Long agentId,
            @RequestBody @Valid CreatePatRequest request,
            @CurrentMember MemberDetails memberDetails) {
        Member agent = agentService.getOwnedActiveAgent(memberDetails.getMemberId(), agentId);
        Duration ttl = request.ttlDays() == null ? null : Duration.ofDays(request.ttlDays());
        GeneratedToken generated = personalAccessTokenService.generate(agent, request.name(), request.scope(), ttl);

        CreatedPatResponse response = CreatedPatResponse.of(generated.rawToken(), PatResponse.from(generated.token()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            operationId = "listAgentTokens",
            summary = "List tokens",
            description = "List the tokens of one of your agents (metadata only).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tokens retrieved"),
        @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<PatResponse>> listTokens(
            @PathVariable Long agentId, @CurrentMember MemberDetails memberDetails) {
        Member agent = agentService.getOwnedActiveAgent(memberDetails.getMemberId(), agentId);
        List<PatResponse> tokens = personalAccessTokenService.listFor(agent.getId()).stream()
                .map(PatResponse::from)
                .toList();

        return ResponseEntity.ok(tokens);
    }

    @Operation(
            operationId = "revokeAgentToken",
            summary = "Revoke a token",
            description = "Revoke a agent token (PAT for agent). It will stop working.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Token revoked"),
        @ApiResponse(responseCode = "404", description = "Agent not found", content = @Content)
    })
    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> revokeToken(
            @PathVariable Long agentId, @PathVariable Long tokenId, @CurrentMember MemberDetails memberDetails) {
        Member agent = agentService.getOwnedActiveAgent(memberDetails.getMemberId(), agentId);
        personalAccessTokenService.revoke(agent.getId(), tokenId);

        return ResponseEntity.noContent().build();
    }
}
