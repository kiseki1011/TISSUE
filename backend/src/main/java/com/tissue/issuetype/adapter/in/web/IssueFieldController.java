package com.tissue.issuetype.adapter.in.web;

import com.tissue.issuetype.adapter.in.dto.request.AddOptionRequest;
import com.tissue.issuetype.adapter.in.dto.request.CreateIssueFieldRequest;
import com.tissue.issuetype.adapter.in.dto.request.PatchIssueFieldRequest;
import com.tissue.issuetype.adapter.in.dto.request.RenameIssueFieldRequest;
import com.tissue.issuetype.adapter.in.dto.request.RenameOptionRequest;
import com.tissue.issuetype.adapter.in.dto.request.ReorderOptionsRequest;
import com.tissue.issuetype.application.dto.request.DeleteIssueFieldCommand;
import com.tissue.issuetype.application.dto.request.DeleteOptionCommand;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.issuetype.application.port.in.IssueFieldUseCase;
import com.tissue.project.adapter.in.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issue-types/{issueTypeId}/issue-fields")
@RequiredArgsConstructor
public class IssueFieldController {

    private final IssueFieldUseCase issueFieldUseCase;

    @PostMapping
    public ResponseEntity<IssueFieldResponse> create(
            @PathVariable Long issueTypeId,
            @RequestBody @Valid CreateIssueFieldRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(issueTypeId, actorContext);
        IssueFieldResponse response = issueFieldUseCase.create(command);

        // TODO: created 사용

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueFieldId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid RenameIssueFieldRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(issueTypeId, issueFieldId, actorContext);
        issueFieldUseCase.rename(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueFieldId}")
    public ResponseEntity<IssueFieldResponse> update(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid PatchIssueFieldRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(issueTypeId, issueFieldId, actorContext);
        issueFieldUseCase.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueFieldId}")
    public ResponseEntity<Void> deleteIssueField(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = new DeleteIssueFieldCommand(issueTypeId, issueFieldId, actorContext);
        issueFieldUseCase.delete(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueFieldId}/options")
    public ResponseEntity<IssueFieldResponse> addIssueFieldOption(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid AddOptionRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(issueTypeId, issueFieldId, actorContext);
        IssueFieldResponse response = issueFieldUseCase.addOption(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> renameIssueFieldOption(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @RequestBody @Valid RenameOptionRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(issueTypeId, issueFieldId, optionId, actorContext);
        issueFieldUseCase.renameOption(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueFieldId}/options")
    public ResponseEntity<ReorderedOptionsResponse> reorderIssueFieldOptions(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid ReorderOptionsRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(issueTypeId, issueFieldId, actorContext);
        ReorderedOptionsResponse response = issueFieldUseCase.reorderOptions(command);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> deleteIssueFieldOption(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = DeleteOptionCommand.builder()
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .optionId(optionId)
                .actorContext(actorContext)
                .build();
        issueFieldUseCase.deleteOption(command);

        return ResponseEntity.noContent().build();
    }
}
