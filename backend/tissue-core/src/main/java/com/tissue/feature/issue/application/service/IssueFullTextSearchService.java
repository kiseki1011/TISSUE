package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.IssueSearchCursor;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueFullTextSearchService implements IssueFullTextSearchUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueFullTextSearchRepository ftsRepository;
    private final IssueSearchPolicy policy;

    @LLMGenerated(
            llmInvolvement = LLMInvolvement.VIBE_CODED,
            model = "claude-opus-4-7-max",
            evaluation = Evaluation.ACCEPTABLE,
            evaluationReason = "Used the cursor implementation (before using keyset).",
            reviewedBy = "kiseki1011")
    @Override
    public CursorPage<IssueSummary> ftsByProjectKeyset(
            ProjectIdentifier pid,
            IssueSearchCondition condition,
            @Nullable String cursor,
            int size,
            Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        if (condition.keyword() == null || condition.keyword().isBlank()) {
            return CursorPage.empty();
        }

        IssueSearchCondition resolved = policy.resolveCurrentSprint(condition, project);
        int clamped = clampSize(size);
        IssueSearchCursor decoded = IssueSearchCursor.decode(cursor);

        List<Issue> fetched = ftsRepository.ftsByProjectAfter(project, resolved, decoded, clamped);
        boolean hasNext = fetched.size() > clamped;
        List<Issue> page = hasNext ? fetched.subList(0, clamped) : fetched;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            Issue last = page.getLast();
            nextCursor = new IssueSearchCursor(last.getPriority(), last.getId()).encode();
        }
        return CursorPage.of(page.stream().map(IssueSummary::from).toList(), nextCursor);
    }

    private static int clampSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
