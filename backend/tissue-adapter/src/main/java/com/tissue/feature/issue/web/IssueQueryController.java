package com.tissue.feature.issue.web;

import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueCustomDetail;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/project/{projectKey}")
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

    // TODO: Consider caching for content
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

    @GetMapping("/issues/{issueKey}/custom-fields")
    public ResponseEntity<IssueCustomDetail> getCustomFields(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @CurrentMember MemberDetails memberDetails) {

        IssueCustomDetail response = issueQueryUseCase.getCustom(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

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

    // TODO: getIssuesByState
    // TODO: getIssuesByStateCategory

    // TODO: getIssues()
    //  - 페이징 쿼리 API(프로젝트 단위)
    //  - Issue를 조건별로 검색 가능
    //  - 조건
    //    - IssuePriority
    //    - dueAt 기간
    //    - startedAt 기간
    //    - resolvedAt 기간
    //    - IssueRelation: 예를 들어서 "ISSUE-123"가 BLOCKING하는 모든 이슈 조회
    //    - 현재 소속 Sprint 번호(예시: "SPRINT-123")
    //    - storyPoint 범위
    //    - progress 범위
    //    - 워크플로우의 특정 state 기준
    //    - currentState의 category
    //    - 해당 조건들에 대한 오름차순, 내림차순이 가능해야 함
    //    - 특정 enum type 커스텀 필드에 대한 특정 선택지로 검색
    //    - (추후 진행)tag를 구현 후에 tag에 따른 검색
    //  - 검색어 조건
    //    - title > content > summary (우선 순위)
    //    - issueKey -> 빠른 속도 검색 가능해야하고 fuzzy matching과 디바운싱도 염두

    // TODO: 특정 WorkspaceMember의 역할에 따른 이슈 목록 검색
    //  - 예) ?participantId={memberId}&role=assignee -> 특정 memberId에 대한 WorkspaceMember가 assignee인
    //   모든 이슈들의 목록
    //  - 당연히 페이징이 가능해야겠지?

    // TODO: getComments(추후 comment 애그리거트 완료 후)
    // TODO: getHistory(추후 도메인 이벤트 도입과 ActivityLog 완료 후)

}
