package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.ProjectMemberInfo;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueSearchUseCase;
import com.tissue.feature.issue.web.request.IssueSearchRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Issue")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueQueryController {

    private final IssueQueryUseCase issueQueryUseCase;
    private final IssueSearchUseCase issueSearchUseCase;

    @Operation(operationId = "searchProjectIssues", summary = "Search project issues", description = """
                    Search and get a list of issues of a project. Supports filtering by priority, \
                    state category/id, assignee, sprint, tags, date ranges, progress percentage, \
                    and keyword (matches issue key and title). Default sort: priority asc, \
                    dueDate asc, storypoint desc.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issues retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid sort property", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @GetMapping("/projects/{projectKey}/issues")
    public ResponseEntity<Page<IssueSummary>> searchProjectIssues(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            IssueSearchRequest request,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<IssueSummary> response = issueSearchUseCase.searchByProject(
                ProjectIdentifier.of(workspaceKey, projectKey),
                request.toCondition(),
                pageable,
                memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueBasic", summary = "Get issue basic info", description = """
                Get an issue's identity, type, current state, priority, author, and assignee.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issue basic info retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/basic")
    public ResponseEntity<IssueBasicInfo> getIssueBasic(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueBasicInfo response =
                issueQueryUseCase.getBasic(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueCommon", summary = "Get issue common fields", description = """
                Get all common fields of an issue (title, content, schedule, progress, participants, etc.).

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issue common fields retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/common")
    public ResponseEntity<IssueCommonDetail> getIssueCommon(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueCommonDetail response = issueQueryUseCase.getCommonFieldValues(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueCustom", summary = "Get issue custom fields", description = """
                Get the issue's custom field values defined by its issue type.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issue custom fields retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/custom")
    public ResponseEntity<IssueCustomDetail> getIssueCustom(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueCustomDetail response = issueQueryUseCase.getCustomFieldValues(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueParent", summary = "Get parent issue identifier", description = """
                Get the parent issue's key and type label. Returns a `null` identifier when the issue has no parent.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Parent identifier retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/parent")
    public ResponseEntity<IssueIdentifierResponse> getIssueParent(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueIdentifierResponse response =
                issueQueryUseCase.getParent(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueChildren", summary = "Get child issue identifiers", description = """
                List the issue's direct child identifiers (one level only).

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Children retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/children")
    public ResponseEntity<List<IssueIdentifierResponse>> getIssueChildren(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<IssueIdentifierResponse> response =
                issueQueryUseCase.getChildren(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueRelations", summary = "Get issue relations", description = """
                Get the issue's outgoing and incoming relations grouped by relation type \
                (`blocks` / `blockedBy` / `duplicates` / `duplicatedBy` / `relevant`).

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relations retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/relations")
    public ResponseEntity<IssueRelationsDetail> getIssueRelations(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueRelationsDetail response =
                issueQueryUseCase.getRelations(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueAuthor", summary = "Get issue author", description = """
                Get the info of the issue's author (may represent a soft-deleted member).

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Author retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/author")
    public ResponseEntity<ProjectMemberInfo> getIssueAuthor(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        ProjectMemberInfo response =
                issueQueryUseCase.getAuthor(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueReviewers", summary = "Get issue reviewers", description = """
                List the issue's reviewers with their review status (`PENDING` / `APPROVED` / `CHANGES_REQUESTED`).

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviewers retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/reviewers")
    public ResponseEntity<IssueReviewersDetail> getIssueReviewers(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueReviewersDetail response =
                issueQueryUseCase.getReviewers(IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueSubscribers", summary = "Get issue subscribers", description = """
                List the issue's subscribers.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscribers retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/subscribers")
    public ResponseEntity<IssueSubscribersDetail> getIssueSubscribers(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        IssueSubscribersDetail response = issueQueryUseCase.getSubscribers(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getIssueAvailableTransitions",
            summary = "Get available workflow transitions",
            description = """
                    List the workflow transitions available from the issue's current state, each \
                    with `canExecute` and `blockedReasons` from guard evaluation so the client can \
                    render disabled buttons.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Available transitions retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/transitions")
    public ResponseEntity<List<TransitionDetail>> getIssueAvailableTransitions(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {
        List<TransitionDetail> response = issueQueryUseCase.getAvailableTransitions(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
