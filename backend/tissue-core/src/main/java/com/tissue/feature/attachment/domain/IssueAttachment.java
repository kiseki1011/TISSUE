package com.tissue.feature.attachment.domain;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.Getter;

@Entity
@Getter
public class IssueAttachment extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(name = "issue_key", nullable = false, updatable = false)
    private String issueKey;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String storedPath;

    @SuppressWarnings("NullAway.Init")
    protected IssueAttachment() {}

    public static IssueAttachment create(
            Issue issue,
            String originalFilename,
            String storedFilename,
            String contentType,
            long fileSize,
            String storedPath) {
        IssueAttachment attachment = new IssueAttachment();
        attachment.issue = issue;
        attachment.ensureEditable();
        attachment.workspaceKey = issue.getWorkspaceKey();
        attachment.issueKey = issue.getKey();
        attachment.originalFilename = originalFilename;
        attachment.storedFilename = storedFilename;
        attachment.contentType = contentType;
        attachment.fileSize = fileSize;
        attachment.storedPath = storedPath;
        return attachment;
    }

    public void ensureEditable() {
        Project project = issue.getProject();
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }

    public boolean isUploader(Long memberId) {
        return Objects.equals(getCreatedBy(), memberId);
    }
}
