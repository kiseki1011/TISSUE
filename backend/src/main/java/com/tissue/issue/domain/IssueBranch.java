package com.tissue.issue.domain;

import com.tissue.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"issue_id", "branch_name"}))
@SQLRestriction("soft_deleted = false")
public class IssueBranch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(nullable = false)
    private String repoUrl;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(nullable = false)
    private String branchUrl;

    private String latestCommitHash;

    private String latestCommitMessage;

    private String latestCommitUrl;

    private String pusherName;

    private LocalDateTime pushedAt;

    @SuppressWarnings("NullAway.Init")
    protected IssueBranch() {
    }

    public static IssueBranch create(
        Issue issue,
        String repoUrl,
        String branchName,
        String branchUrl,
        String latestCommitHash,
        String latestCommitMessage,
        String latestCommitUrl,
        String pusherName,
        LocalDateTime pushedAt) {

        IssueBranch branch = new IssueBranch();
        branch.issue = issue;
        branch.repoUrl = repoUrl;
        branch.branchName = branchName;
        branch.branchUrl = branchUrl;
        branch.latestCommitHash = latestCommitHash;
        branch.latestCommitMessage = latestCommitMessage;
        branch.latestCommitUrl = latestCommitUrl;
        branch.pusherName = pusherName;
        branch.pushedAt = pushedAt;
        return branch;
    }

    public void updateLatestCommit(String hash, String message, String url, String pusher,
        LocalDateTime pushedAt) {
        this.latestCommitHash = hash;
        this.latestCommitMessage = message;
        this.latestCommitUrl = url;
        this.pusherName = pusher;
        this.pushedAt = pushedAt;
    }
}
