package com.tissue.team.adapter.web;

import com.tissue.team.adapter.web.request.CreateTeamRequest;
import com.tissue.team.adapter.web.request.UpdateTeamRequest;
import com.tissue.team.application.dto.response.GetTeams;
import com.tissue.team.application.dto.response.TeamCreateResponse;
import com.tissue.team.application.dto.response.TeamDetail;
import com.tissue.team.application.port.in.TeamUseCase;
import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
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
            @Valid @RequestBody CreateTeamRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {

        var command = request.toCommand(actorContext);
        TeamCreateResponse response = teamUseCase.create(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{teamId}")
                .buildAndExpand(response.teamId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{teamId}")
    public ResponseEntity<Void> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {

        var command = request.toCommand(teamId, actorContext);
        teamUseCase.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId, @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {
        teamUseCase.delete(teamId, actorContext);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetail> getTeamDetail(
            @PathVariable Long teamId, @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {

        TeamDetail response = teamUseCase.getTeam(teamId, actorContext);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GetTeams> getTeams(@CurrentWorkspaceMember WorkspaceMemberContext actorContext) {

        GetTeams response = teamUseCase.getTeams(actorContext);
        return ResponseEntity.ok(response);
    }
}
