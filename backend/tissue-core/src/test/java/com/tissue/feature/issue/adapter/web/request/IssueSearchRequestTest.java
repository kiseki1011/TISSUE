package com.tissue.feature.issue.adapter.web.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IssueSearchRequestTest {

    private static IssueSearchRequest withAssignees(Set<String> assignees) {
        return new IssueSearchRequest(
                null, null, null, null, null, null, assignees, null, null, null, null, null, null, null, null);
    }

    @Test
    void resolvesMeToTheCurrentMemberId() {
        IssueSearchCondition condition = withAssignees(Set.of("me")).toCondition(7L);

        assertThat(condition.assigneeMemberIds()).containsExactly(7L);
    }

    @Test
    void parsesNumericMemberIds() {
        IssueSearchCondition condition = withAssignees(Set.of("42")).toCondition(7L);

        assertThat(condition.assigneeMemberIds()).containsExactly(42L);
    }

    @Test
    void rejectsAValueThatIsNeitherMeNorNumeric() {
        assertThatThrownBy(() -> withAssignees(Set.of("abc")).toCondition(7L))
                .isInstanceOf(BadRequestException.class)
                .extracting(e -> ((BadRequestException) e).getErrorCode())
                .isEqualTo(IssueErrorCode.INVALID_MEMBER_ID_FILTER);
    }
}
