package com.tissue.issuetype.adapter.web;

import com.tissue.issuetype.adapter.web.request.AddOptionRequest;
import com.tissue.issuetype.adapter.web.request.CreateIssueFieldRequest;
import com.tissue.issuetype.adapter.web.request.PatchIssueFieldRequest;
import com.tissue.issuetype.adapter.web.request.RenameIssueFieldRequest;
import com.tissue.issuetype.adapter.web.request.RenameOptionRequest;
import com.tissue.issuetype.adapter.web.request.ReorderOptionsRequest;
import com.tissue.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.issuetype.application.dto.response.ReorderedOptionsResponse;
import com.tissue.issuetype.application.port.in.IssueFieldUseCase;
import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
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

        var command = request.toCommand();
        IssueFieldResponse response = issueFieldUseCase.create(issueTypeId, command, actorContext);

        // TODO: created 사용

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueFieldId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid RenameIssueFieldRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        issueFieldUseCase.rename(issueTypeId, issueFieldId, com.tissue.global.vo.Name.of(request.name()), actorContext);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueFieldId}")
    public ResponseEntity<IssueFieldResponse> update(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid PatchIssueFieldRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand();
        issueFieldUseCase.update(issueTypeId, issueFieldId, command, actorContext);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueFieldId}")
    public ResponseEntity<Void> deleteIssueField(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        issueFieldUseCase.delete(issueTypeId, issueFieldId, actorContext);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueFieldId}/options")
    public ResponseEntity<IssueFieldResponse> addIssueFieldOption(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid AddOptionRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        IssueFieldResponse response = issueFieldUseCase.addOption(
                issueTypeId, issueFieldId, com.tissue.global.vo.Name.of(request.optionName()), actorContext);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> renameIssueFieldOption(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @RequestBody @Valid RenameOptionRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        issueFieldUseCase.renameOption(
                issueTypeId, issueFieldId, optionId, com.tissue.global.vo.Name.of(request.name()), actorContext);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{issueFieldId}/options")
    public ResponseEntity<ReorderedOptionsResponse> reorderIssueFieldOptions(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid ReorderOptionsRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        ReorderedOptionsResponse response =
                issueFieldUseCase.reorderOptions(issueTypeId, issueFieldId, request.targetOrderedIds(), actorContext);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> deleteIssueFieldOption(
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        issueFieldUseCase.deleteOption(issueTypeId, issueFieldId, optionId, actorContext);

        return ResponseEntity.noContent().build();
    }
}
