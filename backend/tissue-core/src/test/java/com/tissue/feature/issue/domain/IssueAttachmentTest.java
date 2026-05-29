package com.tissue.feature.issue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueAttachmentTest {

    @Nested
    @DisplayName("create issue attachment")
    class CreateIssueAttachment {

        @Test
        @DisplayName("success: create attachment")
        void successCreate() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "issue title", IssueHierarchy.STANDARD);

            // when
            IssueAttachment attachment = IssueAttachment.create(
                    issue, "report.pdf", "uuid.pdf", "application/pdf", 1024, "PROJ/PROJ-1/uuid.pdf");

            // then
            assertThat(attachment.getIssue()).isEqualTo(issue);
            assertThat(attachment.getOriginalFilename()).isEqualTo("report.pdf");
            assertThat(attachment.getStoredFilename()).isEqualTo("uuid.pdf");
            assertThat(attachment.getContentType()).isEqualTo("application/pdf");
            assertThat(attachment.getFileSize()).isEqualTo(1024);
            assertThat(attachment.getStoredPath()).isEqualTo("PROJ/PROJ-1/uuid.pdf");
        }

        @Test
        @DisplayName("fail: if project is archived, throws ProjectArchivedException")
        void failCreate_If_ProjectArchived() {
            // given
            Project archivedProject = TestFixtures.archivedProject("PROJ");

            // when & then
            assertThatThrownBy(() -> {
                        Issue issue = TestFixtures.issue(archivedProject, "issue title", IssueHierarchy.STANDARD);
                        IssueAttachment.create(
                                issue, "report.pdf", "uuid.pdf", "application/pdf", 1024, "PROJ/PROJ-1/uuid.pdf");
                    })
                    .isInstanceOf(ProjectArchivedException.class);
        }
    }

    @Nested
    @DisplayName("is attachment uploader")
    class IsAttachmentUploader {

        @Test
        @DisplayName("returns false when createdBy is null")
        void returnsFalseWhenCreatedByIsNull() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "issue title", IssueHierarchy.STANDARD);
            IssueAttachment attachment =
                    IssueAttachment.create(issue, "file.png", "uuid.png", "image/png", 512, "PROJ/PROJ-1/uuid.png");

            // when & then
            assertThat(attachment.isUploader(1L)).isFalse();
        }
    }
}
