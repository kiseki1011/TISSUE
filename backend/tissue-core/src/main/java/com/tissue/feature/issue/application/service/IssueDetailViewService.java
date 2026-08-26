package com.tissue.feature.issue.application.service;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.issue.application.dto.request.IssueDetailSection;
import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.port.usecase.IssueDetailViewUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.PageResponse;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the aggregated issue detail from the per-section queries.
 *
 * <p>Lives in the application layer rather than in a controller because more than one adapter needs the
 * same view: the REST detail screen and the MCP {@code get_issue} tool both read an issue this way, and an
 * assembly owned by one adapter cannot be reached from the other.
 *
 * <p>Reads run in one transaction, so every section sees the same snapshot of the issue.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueDetailViewService implements IssueDetailViewUseCase {

    private final IssueQueryUseCase issueQueryUseCase;
    private final CommentQueryUseCase commentQueryUseCase;

    @Override
    public IssueDetailView getDetailView(
            IssueIdentifier iid, Set<IssueDetailSection> sections, int commentSize, Long actorMemberId) {
        IssueDetail detail = issueQueryUseCase.getDetail(iid, actorMemberId);

        boolean transitions = sections.contains(IssueDetailSection.TRANSITIONS);
        boolean hierarchy = sections.contains(IssueDetailSection.HIERARCHY);
        boolean relations = sections.contains(IssueDetailSection.RELATIONS);
        boolean comments = sections.contains(IssueDetailSection.COMMENTS);
        boolean vcs = sections.contains(IssueDetailSection.VCS);

        return new IssueDetailView(
                detail.common(),
                detail.customFields(),
                transitions ? issueQueryUseCase.getAvailableTransitions(iid, actorMemberId) : List.of(),
                hierarchy ? issueQueryUseCase.getParent(iid, actorMemberId) : IssueIdentifierResponse.asNull(),
                hierarchy ? issueQueryUseCase.getChildren(iid, actorMemberId) : List.of(),
                relations ? issueQueryUseCase.getRelations(iid, actorMemberId) : IssueRelationsDetail.empty(),
                comments ? comments(iid, commentSize, actorMemberId) : PageResponse.from(Page.empty()),
                vcs ? issueQueryUseCase.getBranches(iid, actorMemberId) : List.of(),
                vcs ? issueQueryUseCase.getPullRequests(iid, actorMemberId) : List.of());
    }

    private PageResponse<CommentDetailResponse> comments(IssueIdentifier iid, int size, Long actorMemberId) {
        return PageResponse.from(commentQueryUseCase.getIssueComments(iid, PageRequest.of(0, size), actorMemberId));
    }
}
