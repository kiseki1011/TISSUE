package com.tissue.feature.issue.domain;

import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
@Embeddable
public class IssueParticipants {

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private ProjectMember assignee;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueReviewer> reviewers = new HashSet<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueSubscriber> subscribers = new HashSet<>();

    @SuppressWarnings("NullAway.Init")
    protected IssueParticipants() {}

    public static IssueParticipants of(@Nullable ProjectMember assignee) {
        IssueParticipants participants = new IssueParticipants();
        participants.assignee = assignee;
        return participants;
    }

    void assignTo(ProjectMember assignee) {
        reviewers.removeIf(reviewer -> reviewer.getReviewer().equals(assignee));
        this.assignee = assignee;
    }

    void unassign() {
        this.assignee = null;
    }

    void addReviewer(ProjectMember projectMember, Issue issue) {
        if (assignee != null && assignee.equals(projectMember)) {
            throw new BadRequestException(IssueErrorCode.ASSIGNEE_CANNOT_BE_REVIEWER);
        }
        if (isReviewer(projectMember)) {
            return;
        }
        reviewers.add(new IssueReviewer(projectMember, issue));
    }

    void removeReviewer(ProjectMember projectMember) {
        reviewers.removeIf(r -> r.getReviewer().equals(projectMember));
    }

    void addSubscriber(ProjectMember projectMember, Issue issue) {
        if (isSubscriber(projectMember)) {
            return;
        }
        subscribers.add(new IssueSubscriber(projectMember, issue));
    }

    void removeSubscriber(ProjectMember projectMember) {
        subscribers.removeIf(s -> s.getSubscriber().equals(projectMember));
    }

    int resetReviews(Set<Long> reviewerMemberIds) {
        int count = 0;
        boolean targetSpecific = reviewerMemberIds != null && !reviewerMemberIds.isEmpty();

        for (IssueReviewer reviewer : reviewers) {
            boolean isTarget = targetSpecific
                    ? reviewerMemberIds.contains(reviewer.getReviewer().getMemberId())
                    : reviewer.getStatus() == ReviewStatus.CHANGES_REQUESTED;

            if (isTarget && reviewer.getStatus() != ReviewStatus.PENDING) {
                reviewer.resetReview();
                count++;
            }
        }
        return count;
    }

    boolean isReviewer(ProjectMember projectMember) {
        return reviewers.stream().anyMatch(r -> r.getReviewer().equals(projectMember));
    }

    boolean isSubscriber(ProjectMember projectMember) {
        return subscribers.stream().anyMatch(s -> s.getSubscriber().equals(projectMember));
    }
}
