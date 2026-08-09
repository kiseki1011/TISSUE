package com.tissue.feature.issue.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.feature.comment.application.port.usecase.CommentQueryUseCase;
import com.tissue.feature.issue.application.dto.request.IssueDetailSection;
import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

@ExtendWith(MockitoExtension.class)
class IssueDetailViewServiceTest {

    private static final IssueIdentifier IID = IssueIdentifier.ofIssueKey("PROJ-1");
    private static final Long ACTOR = 7L;

    @Mock
    private IssueQueryUseCase issueQueryUseCase;

    @Mock
    private CommentQueryUseCase commentQueryUseCase;

    @InjectMocks
    private IssueDetailViewService sut;

    @BeforeEach
    void stubDetail() {
        given(issueQueryUseCase.getDetail(IID, ACTOR))
                .willReturn(new IssueDetail(
                        IssueCommonDetail.builder().issueKey("PROJ-1").build(), List.of()));
    }

    @Test
    @DisplayName("success: asking for no section runs only the issue query")
    void skipsEverySectionThatWasNotAskedFor() {
        IssueDetailView view = sut.getDetailView(IID, Set.of(), 20, ACTOR);

        then(issueQueryUseCase).should(never()).getAvailableTransitions(any(), anyLong());
        then(issueQueryUseCase).should(never()).getParent(any(), anyLong());
        then(issueQueryUseCase).should(never()).getChildren(any(), anyLong());
        then(issueQueryUseCase).should(never()).getRelations(any(), anyLong());
        then(issueQueryUseCase).should(never()).getBranches(any(), anyLong());
        then(issueQueryUseCase).should(never()).getPullRequests(any(), anyLong());
        then(commentQueryUseCase).shouldHaveNoInteractions();

        assertThat(view.common().issueKey()).isEqualTo("PROJ-1");
        assertThat(view.availableTransitions()).isEmpty();
        assertThat(view.children()).isEmpty();
        assertThat(view.parent().issueKey()).isNull();
        assertThat(view.relations().blocks()).isEmpty();
        assertThat(view.comments().content()).isEmpty();
        assertThat(view.branches()).isEmpty();
        assertThat(view.pullRequests()).isEmpty();
    }

    @Test
    @DisplayName("success: asking for VCS runs the branch and pull request queries and nothing else")
    void runsOnlyTheRequestedSection() {
        given(issueQueryUseCase.getBranches(IID, ACTOR)).willReturn(List.of());
        given(issueQueryUseCase.getPullRequests(IID, ACTOR)).willReturn(List.of());

        sut.getDetailView(IID, Set.of(IssueDetailSection.VCS), 20, ACTOR);

        then(issueQueryUseCase).should().getBranches(IID, ACTOR);
        then(issueQueryUseCase).should().getPullRequests(IID, ACTOR);
        then(issueQueryUseCase).should(never()).getRelations(any(), anyLong());
        then(commentQueryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("success: asking for every section assembles the whole view")
    void runsEverySectionWhenAllAreRequested() {
        given(issueQueryUseCase.getAvailableTransitions(IID, ACTOR)).willReturn(List.of());
        given(issueQueryUseCase.getParent(IID, ACTOR)).willReturn(null);
        given(issueQueryUseCase.getChildren(IID, ACTOR)).willReturn(List.of());
        given(issueQueryUseCase.getRelations(IID, ACTOR)).willReturn(null);
        given(issueQueryUseCase.getBranches(IID, ACTOR)).willReturn(List.of());
        given(issueQueryUseCase.getPullRequests(IID, ACTOR)).willReturn(List.of());
        given(commentQueryUseCase.getIssueComments(any(), any(), anyLong())).willReturn(Page.empty());

        sut.getDetailView(IID, IssueDetailSection.all(), 20, ACTOR);

        then(issueQueryUseCase).should().getAvailableTransitions(IID, ACTOR);
        then(issueQueryUseCase).should().getParent(IID, ACTOR);
        then(issueQueryUseCase).should().getChildren(IID, ACTOR);
        then(issueQueryUseCase).should().getRelations(IID, ACTOR);
        then(issueQueryUseCase).should().getBranches(IID, ACTOR);
        then(issueQueryUseCase).should().getPullRequests(IID, ACTOR);
        then(commentQueryUseCase).should().getIssueComments(any(), any(), anyLong());
    }

    @Test
    @DisplayName("success: the comment page size reaches the comment query")
    void passesTheRequestedCommentPageSize() {
        given(commentQueryUseCase.getIssueComments(any(), any(), anyLong())).willReturn(Page.empty());

        sut.getDetailView(IID, Set.of(IssueDetailSection.COMMENTS), 5, ACTOR);

        then(commentQueryUseCase)
                .should()
                .getIssueComments(
                        org.mockito.ArgumentMatchers.eq(IID),
                        org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 5),
                        org.mockito.ArgumentMatchers.eq(ACTOR));
    }
}
