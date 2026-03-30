package com.tissue.support;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueContent;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.IssueSchedule;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;

public final class TestFixtures {

    private TestFixtures() {}

    public static Workspace workspace(String key) {
        return Workspace.create(key, key, null);
    }

    public static Member member(String username) {
        return Member.create(username + "@test.com", username, username);
    }

    public static WorkspaceMember workspaceMember(Member member, Workspace workspace, WorkspaceRole role) {
        return WorkspaceMember.create(member, workspace, role);
    }

    public static Project project(Workspace workspace, String key) {
        return Project.create(workspace, key, key, null);
    }

    public static Project archivedProject(Workspace workspace, String key) {
        Project project = Project.create(workspace, key, key, null);
        project.archive();
        return project;
    }

    public static ProjectMember projectMember(Project project, WorkspaceMember workspaceMember) {
        return ProjectMember.create(project, workspaceMember);
    }

    public static ProjectMember projectManager(Project project, WorkspaceMember workspaceMember) {
        return ProjectMember.createManager(project, workspaceMember);
    }

    public static Workflow workflow(Project project) {
        return Workflow.create(project, Name.of("Workflow"), null, ColorType.BLUE);
    }

    public static IssueType issueType(Project project, IssueHierarchy hierarchy) {
        Workflow wf = Workflow.create(project, Name.of("Issue Workflow"), null, ColorType.BLUE);
        wf.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        return IssueType.create(project, Name.of("Story"), null, ColorType.BLACK, IconType.CIRCLE_DOT, hierarchy, wf);
    }

    public static Issue issue(String workspaceKey, String projectKey, String title, IssueHierarchy hierarchy) {
        return issue(project(workspace(workspaceKey), projectKey), title, hierarchy);
    }

    public static Issue issue(Project project, String title, IssueHierarchy hierarchy) {
        return Issue.create(
                project,
                null,
                issueType(project, hierarchy),
                title,
                IssueContent.of(null, null),
                IssueSchedule.of(null),
                IssueParticipants.of(null),
                IssuePriority.NORMAL,
                null,
                null);
    }

    public static Sprint sprint(Project project, String title) {
        return Sprint.create(project, title, null);
    }

    public static Tag tag(Project project, String name) {
        return Tag.create(project, Name.of(name), null, ColorType.RED);
    }

    public static Comment comment(WorkspaceMember author, Issue issue) {
        return Comment.create(author, issue, "test comment", null);
    }
}
