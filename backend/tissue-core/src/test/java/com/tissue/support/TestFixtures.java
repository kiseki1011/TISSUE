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
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;

public final class TestFixtures {

    private TestFixtures() {}

    public static Member member(String username) {
        return Member.create(username + "@test.com", username, username);
    }

    public static Project project(String key) {
        return Project.create(key, key, null);
    }

    public static Project archivedProject(String key) {
        Project project = project(key);
        project.archive();
        return project;
    }

    public static ProjectMember projectMember(Project project, Member member) {
        return ProjectMember.create(project, member);
    }

    public static ProjectMember projectManager(Project project, Member member) {
        return ProjectMember.createManager(project, member);
    }

    public static Workflow workflow() {
        return Workflow.create(Name.of("Workflow"), null, ColorType.BLUE);
    }

    public static IssueType issueType(IssueHierarchy hierarchy) {
        Workflow wf = Workflow.create(Name.of("Issue Workflow"), null, ColorType.BLUE);
        wf.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        return IssueType.create(Name.of("Story"), null, ColorType.BLACK, IconType.CIRCLE_DOT, hierarchy, wf);
    }

    public static Issue issue(Project project, String title, IssueHierarchy hierarchy) {
        return Issue.create(
                project,
                null,
                issueType(hierarchy),
                title,
                IssueContent.of(null, null),
                IssueSchedule.of(null),
                IssueParticipants.of(null),
                IssuePriority.P2,
                null,
                null);
    }

    public static Sprint sprint(Project project, String title) {
        return Sprint.create(project, title, null);
    }

    public static Tag tag(Project project, String name) {
        return Tag.create(project, Name.of(name), null, ColorType.RED);
    }

    public static Comment comment(Member author, Issue issue) {
        return Comment.create(author, issue, "test comment", null);
    }
}
