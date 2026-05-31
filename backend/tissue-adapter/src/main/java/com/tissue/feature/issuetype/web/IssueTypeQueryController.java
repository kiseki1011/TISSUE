package com.tissue.feature.issuetype.web;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeQueryUseCase;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.global.openapi.IssueTypeErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Custom Issue Type")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueTypeQueryController {

    private final IssueTypeQueryUseCase issueTypeQueryUseCase;

    @Operation(operationId = "listIssueTypes", summary = "List issue types", description = """
                    List all global issue types. Each item contains the type's basic info and \
                    its associated workflow. Use `getIssueType` for the full field definitions.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issue types retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @GetMapping("/issue-types")
    public ResponseEntity<List<IssueTypeSummary>> listIssueTypes(@CurrentMember MemberDetails memberDetails) {
        List<IssueTypeSummary> response = issueTypeQueryUseCase.getIssueTypes(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getIssueType", summary = "Get issue type detail", description = """
                    Get a single issue type with its full field definitions, including custom field \
                    options when applicable. You can use this to render an issue create/edit form.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Issue type detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @IssueTypeErrors({IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND})
    @GetMapping("/issue-types/{issueTypeId}")
    public ResponseEntity<IssueTypeDetail> getIssueType(
            @PathVariable Long issueTypeId, @CurrentMember MemberDetails memberDetails) {
        IssueTypeDetail response = issueTypeQueryUseCase.getIssueTypeDetail(issueTypeId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
