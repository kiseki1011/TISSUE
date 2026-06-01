package com.tissue.feature.issuetype.adapter.web;

import com.tissue.feature.issuetype.adapter.web.request.AddOptionRequest;
import com.tissue.feature.issuetype.adapter.web.request.CreateIssueFieldRequest;
import com.tissue.feature.issuetype.adapter.web.request.RenameOptionRequest;
import com.tissue.feature.issuetype.adapter.web.request.UpdateIssueFieldRequest;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.port.usecase.IssueFieldUseCase;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.global.openapi.IssueTypeErrors;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.auth.RequireSystemAdmin;
import com.tissue.shared.vo.Name;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Custom Issue Field")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IssueFieldController {

    private final IssueFieldUseCase issueFieldUseCase;

    @Operation(operationId = "createIssueField", summary = "Create issue field", description = """
                Add a new custom field to an issue type.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issue field created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND,
        IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME,
        IssueTypeErrorCode.OPTION_LIMIT_EXCEEDED,
    })
    @RequireSystemAdmin
    @PostMapping("/issue-types/{issueTypeId}/issue-fields")
    public ResponseEntity<IssueFieldResponse> createIssueField(
            @PathVariable Long issueTypeId,
            @RequestBody @Valid CreateIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        IssueFieldResponse response = issueFieldUseCase.addField(issueTypeId, command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateIssueField", summary = "Update issue field", description = """
                Update an issue field's name, description, or configuration. Only provided fields are updated.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue field updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND,
        IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME,
    })
    @RequireSystemAdmin
    @PatchMapping("/issue-fields/{issueFieldId}")
    public ResponseEntity<Void> updateIssueField(
            @PathVariable Long issueFieldId,
            @RequestBody @Valid UpdateIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        issueFieldUseCase.update(issueFieldId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteIssueField", summary = "Delete issue field", description = """
                Permanently delete a custom field from an issue type.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issue field deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND,
        IssueTypeErrorCode.ISSUE_FIELD_IN_USE,
    })
    @RequireSystemAdmin
    @DeleteMapping("/issue-fields/{issueFieldId}")
    public ResponseEntity<Void> deleteIssueField(
            @PathVariable Long issueFieldId, @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.delete(issueFieldId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addIssueFieldOption", summary = "Add field option", description = """
                Add a new option to a select-type field.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Option added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.ISSUE_FIELD_NOT_FOUND,
        IssueTypeErrorCode.DUPLICATE_FIELD_OPTION_NAME,
        IssueTypeErrorCode.OPTION_LIMIT_EXCEEDED,
        IssueTypeErrorCode.FIELD_TYPE_CANNOT_HAVE_OPTION,
    })
    @RequireSystemAdmin
    @PostMapping("/issue-fields/{issueFieldId}/options")
    public ResponseEntity<IssueFieldResponse> addIssueFieldOption(
            @PathVariable Long issueFieldId,
            @RequestBody @Valid AddOptionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        IssueFieldResponse response =
                issueFieldUseCase.addOption(issueFieldId, Name.of(request.optionName()), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateIssueFieldOption", summary = "Update field option", description = """
                Update an existing option of a select-type field.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.FIELD_OPTION_NOT_FOUND,
        IssueTypeErrorCode.DUPLICATE_FIELD_OPTION_NAME,
    })
    @RequireSystemAdmin
    @PatchMapping("/issue-fields/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> updateIssueFieldOption(
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @RequestBody @Valid RenameOptionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.updateOption(issueFieldId, optionId, Name.of(request.name()), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteIssueFieldOption", summary = "Delete field option", description = """
                Permanently delete an option from a select-type field.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Option deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @IssueTypeErrors({
        IssueTypeErrorCode.FIELD_OPTION_NOT_FOUND,
        IssueTypeErrorCode.ISSUE_FIELD_OPTION_IN_USE,
    })
    @RequireSystemAdmin
    @DeleteMapping("/issue-fields/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> deleteIssueFieldOption(
            @PathVariable Long issueFieldId, @PathVariable Long optionId, @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.deleteOption(issueFieldId, optionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
