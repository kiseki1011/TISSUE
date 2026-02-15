package com.tissue.organization.team.web;

import com.tissue.feature.organization.team.application.dto.response.TeamCreateResponse;
import com.tissue.feature.organization.team.application.dto.response.TeamDetail;
import com.tissue.feature.organization.team.application.dto.response.TeamDetailList;
import com.tissue.feature.organization.team.application.port.usecase.TeamUseCase;
import com.tissue.organization.team.web.request.CreateTeamRequest;
import com.tissue.organization.team.web.request.UpdateTeamRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamUseCase teamUseCase;

    @PostMapping
    public ResponseEntity<TeamCreateResponse> createTeam(
            @PathVariable String workspaceKey,
            @Valid @RequestBody CreateTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        TeamCreateResponse response = teamUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{teamId}")
                .buildAndExpand(response.teamId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<Void> updateTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        teamUseCase.update(workspaceKey, teamId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable String workspaceKey, @PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {

        teamUseCase.delete(workspaceKey, teamId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetail> getTeamDetail(
            @PathVariable String workspaceKey, @PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {

        TeamDetail response = teamUseCase.getTeam(workspaceKey, teamId, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<TeamDetailList> getWorkspaceTeams(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        TeamDetailList response = teamUseCase.getWorkspaceTeams(workspaceKey, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
