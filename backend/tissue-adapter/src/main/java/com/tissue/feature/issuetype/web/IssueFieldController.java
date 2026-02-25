package com.tissue.feature.issuetype.web;

import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.port.usecase.IssueFieldUseCase;
import com.tissue.feature.issuetype.web.request.AddOptionRequest;
import com.tissue.feature.issuetype.web.request.CreateIssueFieldRequest;
import com.tissue.feature.issuetype.web.request.PatchIssueFieldRequest;
import com.tissue.feature.issuetype.web.request.RenameIssueFieldRequest;
import com.tissue.feature.issuetype.web.request.RenameOptionRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
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
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @RequestBody @Valid CreateIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        IssueFieldResponse response = issueFieldUseCase.create(
                ProjectIdentifier.of(workspaceKey, projectKey), issueTypeId, command, memberDetails.getMemberId());

        // TODO: created 사용

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueFieldId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid RenameIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {

        issueFieldUseCase.rename(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                issueFieldId,
                Name.of(request.name()),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{issueFieldId}")
    public ResponseEntity<IssueFieldResponse> update(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid PatchIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        issueFieldUseCase.update(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                issueFieldId,
                command,
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueFieldId}")
    public ResponseEntity<Void> deleteIssueField(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @CurrentMember MemberDetails memberDetails) {

        issueFieldUseCase.delete(
                ProjectIdentifier.of(workspaceKey, projectKey), issueTypeId, issueFieldId, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{issueFieldId}/options")
    public ResponseEntity<IssueFieldResponse> addIssueFieldOption(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid AddOptionRequest request,
            @CurrentMember MemberDetails memberDetails) {

        IssueFieldResponse response = issueFieldUseCase.addOption(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                issueFieldId,
                Name.of(request.optionName()),
                memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> renameIssueFieldOption(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @RequestBody @Valid RenameOptionRequest request,
            @CurrentMember MemberDetails memberDetails) {

        issueFieldUseCase.renameOption(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                issueFieldId,
                optionId,
                Name.of(request.name()),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{issueFieldId}/options/{optionId}")
    public ResponseEntity<Void> deleteIssueFieldOption(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long issueTypeId,
            @PathVariable Long issueFieldId,
            @PathVariable Long optionId,
            @CurrentMember MemberDetails memberDetails) {

        issueFieldUseCase.deleteOption(
                ProjectIdentifier.of(workspaceKey, projectKey),
                issueTypeId,
                issueFieldId,
                optionId,
                memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
