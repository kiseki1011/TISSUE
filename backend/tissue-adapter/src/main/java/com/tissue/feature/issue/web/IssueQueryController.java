package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.feature.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}")
@RequiredArgsConstructor
public class IssueQueryController {

    private final IssueQueryUseCase issueQueryUseCase;

    @GetMapping("/issues/{issueKey}/basic")
    public ResponseEntity<IssueBasicInfo> getBasicInfo(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueBasicInfo response = issueQueryUseCase.getBasic(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}")
    public ResponseEntity<IssueCommonDetail> getCommon(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueCommonDetail response = issueQueryUseCase.getCommon(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    //    @GetMapping("/issues/{issueKey}/custom-fields")
    //    public ResponseEntity<IssueCustomDetail> getCustomFields(
    //            @PathVariable String workspaceKey,
    //            @PathVariable String projectKey,
    //            @PathVariable String issueKey,
    //            @CurrentMember MemberDetails memberDetails) {
    //
    //        IssueCustomDetail response = issueQueryUseCase.getCustom(
    //                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
    //        return ResponseEntity.ok(response);
    //    }

    @GetMapping("/issues/{issueKey}/parent")
    public ResponseEntity<IssueIdentifierResponse> getParent(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueIdentifierResponse response = issueQueryUseCase.getParent(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}/children")
    public ResponseEntity<List<IssueIdentifierResponse>> getChildren(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        List<IssueIdentifierResponse> response = issueQueryUseCase.getChildren(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}/relations")
    public ResponseEntity<IssueRelationsDetail> getRelations(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueRelationsDetail response = issueQueryUseCase.getRelations(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}/author")
    public ResponseEntity<ParticipantInfo> getAuthor(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        ParticipantInfo response = issueQueryUseCase.getAuthor(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}/reviewers")
    public ResponseEntity<IssueReviewersDetail> getReviewers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueReviewersDetail response = issueQueryUseCase.getReviewers(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}/subscribers")
    public ResponseEntity<IssueSubscribersDetail> getSubscribers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueSubscribersDetail response = issueQueryUseCase.getSubscribers(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/issues/{issueKey}/transitions")
    public ResponseEntity<List<TransitionDetail>> getAvailableTransitions(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        List<TransitionDetail> response = issueQueryUseCase.getAvailableTransitions(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
