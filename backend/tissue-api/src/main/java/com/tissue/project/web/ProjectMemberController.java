package com.tissue.project.web;

import com.tissue.feature.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.feature.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.feature.project.application.port.usecase.ProjectMemberUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.project.web.request.AddProjectMembersRequest;
import com.tissue.shared.dto.ProjectIdentifier;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/members")
public class ProjectMemberController {

    private final ProjectMemberUseCase commandUseCase;

    @PostMapping("/batch")
    public ResponseEntity<ProjectMembersCommandResult> addMembers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid AddProjectMembersRequest request,
            @CurrentMember MemberDetails memberDetails) {

        ProjectMembersCommandResult response = commandUseCase.addMembers(
                ProjectIdentifier.of(workspaceKey, projectKey), request.targetMemberIds(), memberDetails.getMemberId());

        // TODO: use created?

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<ProjectMemberCommandResult> joinProjectDirectly(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        ProjectMemberCommandResult response =
                commandUseCase.join(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.kickMember(
                ProjectIdentifier.of(workspaceKey, projectKey), targetMemberId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> leaveProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        commandUseCase.leave(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
