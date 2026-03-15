package com.tissue.feature.issue.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueTest {

    @Nested
    @DisplayName("set parent issue")
    class SetParentIssue {

        @Test
        @DisplayName("fail: fails if try to set itself as parent")
        void failsWhenParentSelfReference() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue issue = TestFixtures.issue(project, "test issue", IssueHierarchy.STANDARD);

            // when & then
            assertThatThrownBy(() -> issue.setParentIssue(issue)).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("success: sets parent if 1 hierarchy higher than child")
        void successWhenParent_Is_1_HierarchyHigher() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue issue = TestFixtures.issue(project, "test issue", IssueHierarchy.STANDARD);
            Issue parent = TestFixtures.issue(project, "test issue", IssueHierarchy.EPIC);

            // when
            issue.setParentIssue(parent);

            // then
            assertThat(issue.getParentIssue().getHierarchy()).isEqualTo(IssueHierarchy.EPIC);
        }

        @Test
        @DisplayName("fail: rejects parent if not 1 hierarchy higher than child")
        void failWhenParent_IsNot_1_HierarchyHigher() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue issue = TestFixtures.issue(project, "test issue", IssueHierarchy.SUBTASK);
            // set parent as 2 hierarchy higher
            Issue parent = TestFixtures.issue(project, "test issue", IssueHierarchy.EPIC);

            // when & then
            assertThatThrownBy(() -> issue.setParentIssue(parent)).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("success: cross project parent is allowed if child issue is 'STANDARD' hierarchy")
        void crossProjectSuccess_When_ChildIssueStandard() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Project parentProject = TestFixtures.project(ws, "PROJ2");
            Issue issue = TestFixtures.issue(project, "test issue", IssueHierarchy.STANDARD);
            Issue parent = TestFixtures.issue(parentProject, "test issue", IssueHierarchy.EPIC);

            // when
            issue.setParentIssue(parent);

            // then
            assertThat(issue.getParentIssue().getHierarchy()).isEqualTo(IssueHierarchy.EPIC);
        }

        @Test
        @DisplayName("fail: cross project parent fails if child issue is 'SUBTASK' hierarchy")
        void crossProjectFail_When_ChildIssueSubTask() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Project parentProject = TestFixtures.project(ws, "PROJ2");
            Issue issue = TestFixtures.issue(project, "test issue", IssueHierarchy.SUBTASK);
            Issue parent = TestFixtures.issue(parentProject, "test issue", IssueHierarchy.STANDARD);

            // when & then
            assertThatThrownBy(() -> issue.setParentIssue(parent)).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("fail: cross workspace parent is not allowed")
        void crossWorkspaceNotAllowed() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Workspace parentWs = TestFixtures.workspace("P-WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Project parentProject = TestFixtures.project(parentWs, "PROJ2");
            Issue issue = TestFixtures.issue(project, "test issue", IssueHierarchy.STANDARD);
            Issue parent = TestFixtures.issue(parentProject, "test issue", IssueHierarchy.EPIC);

            // when & then
            assertThatThrownBy(() -> issue.setParentIssue(parent)).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("remove parent issue")
    class RemoveParentIssue {

        @Test
        @DisplayName("fail: rejects parent removal for SUBTASK hierarchy")
        void failWhenSubtaskHierarchy() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue parent = TestFixtures.issue(project, "parent", IssueHierarchy.STANDARD);
            Issue subtask = TestFixtures.issue(project, "subtask", IssueHierarchy.SUBTASK);
            subtask.setParentIssue(parent);

            // when & then
            assertThatThrownBy(subtask::removeParentIssue).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("fail: rejects parent removal for MICROTASK hierarchy")
        void failWhenMicrotaskHierarchy() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue parent = TestFixtures.issue(project, "parent", IssueHierarchy.SUBTASK);
            Issue microtask = TestFixtures.issue(project, "microtask", IssueHierarchy.MICROTASK);
            microtask.setParentIssue(parent);

            // when & then
            assertThatThrownBy(microtask::removeParentIssue).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("success: allows parent removal for STANDARD hierarchy")
        void successWhenStandardHierarchy() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project project = TestFixtures.project(ws, "PROJ");
            Issue parent = TestFixtures.issue(project, "parent", IssueHierarchy.EPIC);
            Issue standard = TestFixtures.issue(project, "standard", IssueHierarchy.STANDARD);
            standard.setParentIssue(parent);

            // when
            standard.removeParentIssue();

            // then
            assertThat(standard.getParentIssue()).isNull();
        }
    }

    @Nested
    @DisplayName("update story point")
    class UpdateStoryPoint {

        @Test
        @DisplayName("fail: rejects story point update for EPIC hierarchy")
        void failWhenEpicHierarchy() {
            // given
            Issue epic = TestFixtures.issue("WORKSPACE", "PROJ", "epic", IssueHierarchy.EPIC);

            // when & then
            assertThatThrownBy(() -> epic.updateStoryPoint(5)).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("fail: rejects story point update for SUBTASK hierarchy")
        void failWhenSubtaskHierarchy() {
            // given
            Issue subtask = TestFixtures.issue("WORKSPACE", "PROJ", "subtask", IssueHierarchy.SUBTASK);

            // when & then
            assertThatThrownBy(() -> subtask.updateStoryPoint(5)).isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("success: allows story point update for STANDARD hierarchy")
        void successWhenStandardHierarchy() {
            // given
            Issue standard = TestFixtures.issue("WORKSPACE", "PROJ", "standard", IssueHierarchy.STANDARD);

            // when
            standard.updateStoryPoint(5);

            // then
            assertThat(standard.getStoryPoint()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("validate issue is editable")
    class ValidateEditable {

        @Test
        @DisplayName("fail: throws ProjectArchivedException if project is archived")
        void failWhenProjectArchived() {
            // given
            Workspace ws = TestFixtures.workspace("WORKSPACE");
            Project archivedProject = TestFixtures.archivedProject(ws, "PROJ");

            // when & then
            assertThatThrownBy(() -> TestFixtures.issue(archivedProject, "issue", IssueHierarchy.STANDARD))
                    .isInstanceOf(ProjectArchivedException.class);
        }
    }
}
