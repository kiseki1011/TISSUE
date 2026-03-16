package com.tissue.feature.issue.domain.service.calculator;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueProgressCalculatorTest {

    private final IssueProgressCalculator sut = new IssueProgressCalculator();

    @Nested
    @DisplayName("calculate and update progress")
    class CalculateAndUpdateProgress {

        @Test
        @DisplayName("success: calculates count-based progress for non EPIC")
        void successCountBasedForNonEpic() {
            // given
            Issue issue = mock(Issue.class);
            given(issue.getHierarchy()).willReturn(IssueHierarchy.STANDARD);

            // when
            sut.calculateAndUpdateProgress(issue, 3, 10, 0, 0);

            // then
            then(issue).should().updateProgress(30, null);
        }

        @Test
        @DisplayName("success: calculates both count-based and point-based progress for EPIC")
        void successBothForEpic() {
            // given
            Issue issue = mock(Issue.class);
            given(issue.getHierarchy()).willReturn(IssueHierarchy.EPIC);

            // when
            sut.calculateAndUpdateProgress(issue, 5, 10, 8, 20);

            // then
            then(issue).should().updateProgress(50, 40);
        }

        @Test
        @DisplayName("success: returns 0 percent when total is zero")
        void successZeroPercent() {
            // given
            Issue issue = mock(Issue.class);
            given(issue.getHierarchy()).willReturn(IssueHierarchy.STANDARD);

            // when
            sut.calculateAndUpdateProgress(issue, 0, 0, 0, 0);

            // then
            then(issue).should().updateProgress(0, null);
        }
    }

    @Nested
    @DisplayName("calculate and update epic story point")
    class CalculateAndUpdateEpicStoryPoint {

        @Test
        @DisplayName("success: updates epic story point from total story points of children")
        void successUpdateEpicStoryPoint() {
            // given
            Issue issue = mock(Issue.class);
            given(issue.getHierarchy()).willReturn(IssueHierarchy.EPIC);

            // when
            sut.calculateAndUpdateEpicStoryPoint(issue, 42);

            // then
            then(issue).should().recalculateEpicStoryPoint(42);
        }

        @Test
        @DisplayName("success: skips story point update for non EPIC")
        void successSkipForNonEpic() {
            // given
            Issue issue = mock(Issue.class);
            given(issue.getHierarchy()).willReturn(IssueHierarchy.STANDARD);

            // when
            sut.calculateAndUpdateEpicStoryPoint(issue, 42);

            // then
            then(issue).should(never()).recalculateEpicStoryPoint(42);
        }
    }
}
