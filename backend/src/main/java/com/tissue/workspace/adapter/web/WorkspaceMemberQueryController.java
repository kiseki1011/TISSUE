package com.tissue.workspace.adapter.web;

import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.workspace.application.port.in.WorkspaceMemberQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
@RequiredArgsConstructor
public class WorkspaceMemberQueryController {

    private final WorkspaceMemberQueryUseCase workspaceMemberQueryUseCase;

    @GetMapping("/search")
    public ResponseEntity<List<WorkspaceMemberSearchResponse>> searchMembers(
        @PathVariable String workspaceKey,
        @CurrentWorkspaceMember WorkspaceMemberContext context,
        @RequestParam String query,
        @RequestParam(required = false) @Nullable String projectKey) {

        return ResponseEntity.ok(
            workspaceMemberQueryUseCase.searchMembers(context, query, projectKey));
    }
}
