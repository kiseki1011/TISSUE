package com.tissue.feature.issuetype.web;

import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.port.usecase.IssueFieldUseCase;
import com.tissue.feature.issuetype.web.request.AddOptionRequest;
import com.tissue.feature.issuetype.web.request.CreateIssueFieldRequest;
import com.tissue.feature.issuetype.web.request.PatchIssueFieldRequest;
import com.tissue.feature.issuetype.web.request.RenameIssueFieldRequest;
import com.tissue.feature.issuetype.web.request.RenameOptionRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.vo.Name;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Custom Issue Field")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueFieldController {

    private final IssueFieldUseCase issueFieldUseCase;

    @Operation(summary = "Create issue field", description = """
                Add a new custom field to an issue type.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue field created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue type not found", content = @Content)
    })
    @PostMapping("issue-types/{issueTypeId}/issue-fields")
    public ResponseEntity<IssueFieldResponse> create(
            @PathVariable String workspaceKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid CreateIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        IssueFieldResponse response =
                issueFieldUseCase.addField(workspaceKey, issueTypeId, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.issueFieldId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Rename issue field", description = """
                Rename an existing custom field.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue field renamed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue field not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Field name already exists", content = @Content)
    })
    @PutMapping("issue-fields/{issueFieldId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid RenameIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.rename(workspaceKey, issueFieldId, Name.of(request.name()), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update issue field", description = """
                Update an issue field's description or configuration.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue field updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue field not found", content = @Content)
    })
    @PatchMapping("issue-fields/{issueFieldId}")
    public ResponseEntity<IssueFieldResponse> update(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid PatchIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        issueFieldUseCase.update(workspaceKey, issueFieldId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete issue field", description = """
                Delete a custom field from an issue type.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue field deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue field not found", content = @Content)
    })
    @DeleteMapping("issue-fields/{issueFieldId}")
    public ResponseEntity<Void> deleteIssueField(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.delete(workspaceKey, issueFieldId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add field option", description = """
                Add a new option to a select-type field.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Option added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue field not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Option name already exists", content = @Content)
    })
    @PostMapping("issue-fields/{issueFieldId}/options")
    public ResponseEntity<IssueFieldResponse> addIssueFieldOption(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid AddOptionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        IssueFieldResponse response = issueFieldUseCase.addOption(
                workspaceKey, issueFieldId, Name.of(request.optionName()), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Rename field option", description = """
                Rename an existing option of a select-type field.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option renamed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue field or option not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Option name already exists", content = @Content)
    })
    @PutMapping("issue-fields/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> renameIssueFieldOption(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @RequestBody @Valid RenameOptionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.renameOption(
                workspaceKey, issueFieldId, optionId, Name.of(request.name()), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete field option", description = """
                Delete an option from a select-type field.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Issue field or option not found", content = @Content)
    })
    @DeleteMapping("issue-fields/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> deleteIssueFieldOption(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.deleteOption(workspaceKey, issueFieldId, optionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
