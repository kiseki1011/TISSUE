package com.tissue.feature.issue.adapter.web;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.issue.adapter.web.request.IssueSearchRequest;
import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.ProjectMemberInfo;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.PageResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Issue")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueQueryController {

    private final IssueQueryUseCase issueQueryUseCase;
    private final IssueFullTextSearchUseCase issueFtsUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    @Operation(operationId = "searchProjectIssues", summary = "Search project issues", description = """
                    Search issues in a project by keyword. The keyword is matched against the issue's \
                    key, title, and content.

                    Keyword search can be combined with the regular issue filters \
                    (priority, state, assignee, sprint, tags, date ranges, etc.) - pass them as query \
                    parameters alongside `keyword`.

                    **Pagination (offset-based):**
                    - `page` is the zero-based page index (default 0).
                    - `size` controls page size (default 20, max 100).
                    - Results are sorted by relevance (text-match score), then by priority, then \
                    by most recent first. The `sort` query parameter is ignored.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issues retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND, ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("/projects/{projectKey}/issues:search")
    public ResponseEntity<PageResponse<IssueSummary>> searchProjectIssues(
            @PathVariable String projectKey,
            @ParameterObject IssueSearchRequest request,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @CurrentMember MemberDetails memberDetails) {
        Page<IssueSummary> response = issueFtsUseCase.ftsByProjectRanked(
                ProjectIdentifier.ofProjectKey(projectKey),
                request.toCondition(memberDetails.getMemberId()),
                page,
                size,
                memberDetails.getMemberId());
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @Operation(operationId = "searchAllIssues", summary = "Search issues across my projects", description = """
                    Full-text search across issues in every project (instance-wide) the caller is a member of. \
                    Same `keyword` and filters as the project search. Results are \
                    restricted to the caller's project memberships.

                    **Pagination (offset-based):**
                    - `page` is the zero-based page index (default 0).
                    - `size` controls page size (default 20, max 100).
                    - Results are sorted by relevance, then priority, then most recent. The `sort` \
                    query parameter is ignored.

                    **Requirements:**
                    - Results scoped to the caller's project memberships""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issues retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @GetMapping("/issues:search")
    public ResponseEntity<PageResponse<IssueSummary>> searchAllIssues(
            @ParameterObject IssueSearchRequest request,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @CurrentMember MemberDetails memberDetails) {
        Page<IssueSummary> response = issueFtsUseCase.ftsAllRanked(
                request.toCondition(memberDetails.getMemberId()), page, size, memberDetails.getMemberId());
        return ResponseEntity.ok(PageResponse.from(response));
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueBasicInfo response =
                issueQueryUseCase.getBasic(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueCommonDetail response = issueQueryUseCase.getCommonFieldValues(
                IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueDetailView", summary = "Get aggregated issue detail", description = """
                Get everything the issue detail view needs in one response: common fields, custom \
                fields (with option names resolved), available transitions (with their target state), \
                parent, children, relations, and the first page of comments. Lets a client render the \
                whole detail screen without a separate call per section.

                **Pagination:**
                - `commentSize` is how many root comments to embed (default 20).

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issue detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
    @GetMapping("/issues/{issueKey}/detail")
    public ResponseEntity<IssueDetailView> getIssueDetailView(
            @PathVariable String issueKey,
            @RequestParam(value = "commentSize", defaultValue = "20") int commentSize,
            @CurrentMember MemberDetails memberDetails) {
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);
        Long actorMemberId = memberDetails.getMemberId();

        IssueDetail detail = issueQueryUseCase.getDetail(iid, actorMemberId);
        Page<CommentDetailResponse> comments =
                commentQueryUseCase.getIssueComments(iid, PageRequest.of(0, commentSize), actorMemberId);

        IssueDetailView response = new IssueDetailView(
                detail.common(),
                detail.customFields(),
                issueQueryUseCase.getAvailableTransitions(iid, actorMemberId),
                issueQueryUseCase.getParent(iid, actorMemberId),
                issueQueryUseCase.getChildren(iid, actorMemberId),
                issueQueryUseCase.getRelations(iid, actorMemberId),
                PageResponse.from(comments));
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueCustomDetail response = issueQueryUseCase.getCustomFieldValues(
                IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueIdentifierResponse response =
                issueQueryUseCase.getParent(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        List<IssueIdentifierResponse> response =
                issueQueryUseCase.getChildren(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueRelationsDetail response =
                issueQueryUseCase.getRelations(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        ProjectMemberInfo response =
                issueQueryUseCase.getAuthor(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueReviewersDetail response =
                issueQueryUseCase.getReviewers(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        IssueSubscribersDetail response =
                issueQueryUseCase.getSubscribers(IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
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
            @PathVariable String issueKey, @CurrentMember MemberDetails memberDetails) {
        List<TransitionDetail> response = issueQueryUseCase.getAvailableTransitions(
                IssueIdentifier.ofIssueKey(issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
