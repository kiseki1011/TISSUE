package com.tissue.issue.domain;

import com.tissue.issue.domain.enums.ReviewStatus;
import com.tissue.project.domain.ProjectMember;
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

@Embeddable
@Getter
public class IssueParticipants {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private ProjectMember reporter;

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

    public static IssueParticipants of(ProjectMember reporter, @Nullable ProjectMember assignee) {
        IssueParticipants participants = new IssueParticipants();
        participants.reporter = reporter;
        participants.assignee = assignee;

        return participants;
    }

    void changeReporter(ProjectMember reporter) {
        this.reporter = reporter;
    }

    void assignTo(ProjectMember assignee) {
        this.assignee = assignee;
    }

    void unassign() {
        this.assignee = null;
    }

    void addReviewer(ProjectMember projectMember, Issue issue) {
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

    void clear() {
        unassign();
        this.reviewers.clear();
        this.subscribers.clear();
    }

    boolean isReporter(ProjectMember projectMember) {
        return reporter.equals(projectMember);
    }

    boolean isAssignee(ProjectMember projectMember) {
        return assignee != null && assignee.equals(projectMember);
    }

    boolean isReviewer(ProjectMember projectMember) {
        return reviewers.stream().anyMatch(r -> r.getReviewer().equals(projectMember));
    }

    boolean isSubscriber(ProjectMember projectMember) {
        return subscribers.stream().anyMatch(s -> s.getSubscriber().equals(projectMember));
    }
}
