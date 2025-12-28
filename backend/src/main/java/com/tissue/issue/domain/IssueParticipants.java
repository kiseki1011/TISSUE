package com.tissue.issue.domain;

import com.tissue.project.domain.ProjectMember;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.lang.Nullable;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueParticipants {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private ProjectMember reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private ProjectMember assignee;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueReviewer> reviewers = new HashSet<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueSubscriber> subscribers = new HashSet<>();

    public static IssueParticipants of(
            @NonNull ProjectMember reporter, @Nullable ProjectMember assignee) {
        IssueParticipants participants = new IssueParticipants();
        participants.reporter = reporter;
        participants.assignee = assignee;

        return participants;
    }

    void changeReporter(@NonNull ProjectMember reporter) {
        this.reporter = reporter;
    }

    void assignTo(@NonNull ProjectMember assignee) {
        this.assignee = assignee;
    }

    void unassign() {
        this.assignee = null;
    }

    void addReviewer(@NonNull ProjectMember projectMember, @NonNull Issue issue) {
        if (isReviewer(projectMember)) {
            return;
        }
        reviewers.add(new IssueReviewer(projectMember, issue));
    }

    void removeReviewer(@NonNull ProjectMember projectMember) {
        reviewers.removeIf(r -> r.getReviewer().equals(projectMember));
    }

    void addSubscriber(@NonNull ProjectMember projectMember, @NonNull Issue issue) {
        if (isSubscriber(projectMember)) {
            return;
        }
        subscribers.add(new IssueSubscriber(projectMember, issue));
    }

    void removeSubscriber(@NonNull ProjectMember projectMember) {
        subscribers.removeIf(s -> s.getSubscriber().equals(projectMember));
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
