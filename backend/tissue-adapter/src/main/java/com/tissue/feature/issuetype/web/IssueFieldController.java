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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class IssueFieldController {

    private final IssueFieldUseCase issueFieldUseCase;

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

    @PutMapping("issue-fields/{issueFieldId}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @RequestBody @Valid RenameIssueFieldRequest request,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.rename(workspaceKey, issueFieldId, Name.of(request.name()), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

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

    @DeleteMapping("issue-fields/{issueFieldId}")
    public ResponseEntity<Void> deleteIssueField(
            @PathVariable String workspaceKey,
            @PathVariable Long issueFieldId,
            @CurrentMember MemberDetails memberDetails) {
        issueFieldUseCase.delete(workspaceKey, issueFieldId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

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
