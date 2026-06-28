package com.tissue.feature.tag.application.service;

import static com.tissue.feature.project.domain.exception.ProjectErrorCode.PROJECT_MANAGER_REQUIRED;
import static com.tissue.feature.tag.domain.exception.TagErrorCode.DUPLICATE_TAG_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.application.port.repository.IssueTagRepository;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.port.repository.TagRepository;
import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private ProjectAccessResolver projectAccessResolver;

    @Mock
    private ProjectFinder projectFinder;

    @Mock
    private TagFinder tagFinder;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagValidator tagValidator;

    @Mock
    private IssueTagRepository issueTagRepository;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    private TagCommandService sut;

    @Nested
    @DisplayName("create tag")
    class CreateTag {

        @Test
        @DisplayName("success: create and save tag")
        void successCreateTag() {
            // given
            Long actorMemberId = 1L;
            ProjectIdentifier pid = new ProjectIdentifier("PROJ");

            ProjectMember actor = mock(ProjectMember.class);
            Project project = mock(Project.class);
            Name tagName = Name.of("deployment");

            CreateTagCommand cmd = new CreateTagCommand(tagName, "release and deployment related", ColorType.ANSI_BLUE);

            given(projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(pid.projectKey())).willReturn(project);

            // when
            sut.create(pid, cmd, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(tagValidator).should().ensureUniqueName(project, tagName);
            then(tagRepository).should().save(any(Tag.class));
        }

        @Test
        @DisplayName("fail: throws ResourceConflictException when tag name is duplicate in project scope")
        void failCreateTag_If_DuplicateNameExists() {
            // given
            Long actorMemberId = 1L;
            ProjectIdentifier pid = new ProjectIdentifier("PROJ");

            ProjectMember actor = mock(ProjectMember.class);
            Project project = mock(Project.class);
            Name tagName = Name.of("deployment");

            CreateTagCommand cmd = new CreateTagCommand(tagName, "desc", ColorType.ANSI_BLUE);

            given(projectAccessResolver.resolveByProjectKey(pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(pid.projectKey())).willReturn(project);

            willThrow(new ResourceConflictException(DUPLICATE_TAG_NAME))
                    .given(tagValidator)
                    .ensureUniqueName(project, tagName);

            // when & then
            assertThatThrownBy(() -> sut.create(pid, cmd, actorMemberId)).isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("delete tag")
    class DeleteTag {

        @Test
        @DisplayName("success: delete tag and cascade delete issue tags")
        void successDeleteTag() {
            // given
            Long actorMemberId = 1L;
            Long tagId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Tag tag = mock(Tag.class);
            Project project = mock(Project.class);

            given(tagFinder.getWithProject(tagId)).willReturn(tag);
            given(tag.getProject()).willReturn(project);
            given(project.getKey()).willReturn("PROJ");
            given(projectAccessResolver.resolveByProjectKey("PROJ", actorMemberId))
                    .willReturn(actor);

            // when
            sut.delete(tagId, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(issueTagRepository).should().deleteAllByTag(tag);
            then(tagRepository).should().delete(tag);
        }

        @Test
        @DisplayName("fail: throws ForbiddenException if actor is not 'ProjectRole.MANAGER'")
        void failDeleteTag_If_NotProjectManager() {
            // given
            Long actorMemberId = 1L;
            Long tagId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Tag tag = mock(Tag.class);
            Project project = mock(Project.class);

            given(tagFinder.getWithProject(tagId)).willReturn(tag);
            given(tag.getProject()).willReturn(project);
            given(project.getKey()).willReturn("PROJ");
            given(projectAccessResolver.resolveByProjectKey("PROJ", actorMemberId))
                    .willReturn(actor);

            willThrow(new ForbiddenException(PROJECT_MANAGER_REQUIRED))
                    .given(projectAuthorizationService)
                    .requireProjectManager(actor);

            // when & then
            assertThatThrownBy(() -> sut.delete(tagId, actorMemberId)).isInstanceOf(ForbiddenException.class);
        }
    }
}
