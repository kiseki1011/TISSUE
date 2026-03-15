package com.tissue.feature.issuetype.application.service;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_FIELD_IN_USE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.service.finder.IssueFieldFinder;
import com.tissue.feature.issuetype.application.service.finder.IssueTypeFinder;
import com.tissue.feature.issuetype.application.service.validator.IssueFieldValidator;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueFieldServiceTest {

    @Mock
    private IssueTypeFinder issueTypeFinder;

    @Mock
    private IssueFieldFinder issueFieldFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private IssueFieldRepository issueFieldRepository;

    @Mock
    private IssueFieldValidator issueFieldValidator;

    @Mock
    private IssuePolicy issuePolicy;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    private IssueFieldService sut;

    @Nested
    @DisplayName("add issue field")
    class AddIssueField {

        @ParameterizedTest
        @EnumSource(value = IssueFieldType.class, names = {"SELECT_OPTION", "CHECKLIST"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("success: add issue field without options")
        void successAddIssueField(IssueFieldType fieldType) {
            // given
            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            Name fieldName = Name.of("goal");

            CreateIssueFieldCommand cmd = CreateIssueFieldCommand.builder()
                    .name(fieldName)
                    .issueFieldType(fieldType)
                    .required(false)
                    .position(0)
                    .build();

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            IssueField issueField = mock(IssueField.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueTypeFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), issueTypeId))
                    .willReturn(issueType);
            given(issueType.addField(fieldName, cmd.description(), fieldType, cmd.required(), cmd.position()))
                    .willReturn(issueField);
            given(issueField.getIssueFieldType()).willReturn(fieldType);
            given(issueFieldRepository.save(any(IssueField.class))).willReturn(issueField);

            // when
            sut.addField(pid, issueTypeId, cmd, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(issueFieldValidator).should().ensureUniqueLabel(issueType, fieldName);
            then(issueType).should().addField(fieldName, cmd.description(), fieldType, cmd.required(), cmd.position());
            then(issueFieldRepository).should().save(any(IssueField.class));
            then(issuePolicy).shouldHaveNoInteractions();
        }

        @ParameterizedTest
        @EnumSource(value = IssueFieldType.class, names = {"SELECT_OPTION", "CHECKLIST"})
        @DisplayName("success: add issue field with initial options")
        void successAddIssueFieldWithOptions(IssueFieldType fieldType) {
            // given
            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            Name fieldName = Name.of("status");
            List<Name> initialOptions = List.of(Name.of("option1"), Name.of("option2"));

            CreateIssueFieldCommand cmd = CreateIssueFieldCommand.builder()
                    .name(fieldName)
                    .issueFieldType(fieldType)
                    .required(false)
                    .position(0)
                    .initialOptions(initialOptions)
                    .build();

            ProjectMember actor = mock(ProjectMember.class);
            IssueType issueType = mock(IssueType.class);
            IssueField issueField = mock(IssueField.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueTypeFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), issueTypeId))
                    .willReturn(issueType);
            given(issueType.addField(fieldName, cmd.description(), fieldType, cmd.required(), cmd.position()))
                    .willReturn(issueField);
            given(issueField.getIssueFieldType()).willReturn(fieldType);
            given(issueFieldRepository.save(any(IssueField.class))).willReturn(issueField);

            // when
            sut.addField(pid, issueTypeId, cmd, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(issueFieldValidator).should().ensureUniqueLabel(issueType, fieldName);
            then(issuePolicy).should().ensureCanAddOption(initialOptions.size());
            then(issueField).should().addOption(Name.of("option1"));
            then(issueField).should().addOption(Name.of("option2"));
            then(issueFieldRepository).should().save(any(IssueField.class));
        }
    }

    @Nested
    @DisplayName("delete issue field")
    class DeleteIssueField {

        @Test
        @DisplayName("success: delete issue field")
        void successDeleteIssueField() {
            // given
            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            Long issueFieldId = 10L;

            ProjectMember actor = mock(ProjectMember.class);
            IssueField issueField = mock(IssueField.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFieldFinder.getWithProjectAndIssueTypeBy(
                            pid.workspaceKey(), pid.projectKey(), issueTypeId, issueFieldId))
                    .willReturn(issueField);

            // when
            sut.delete(pid, issueTypeId, issueFieldId, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(issueFieldValidator).should().ensureDeletable(issueField);
            then(issueFieldRepository).should().delete(issueField);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if issue field is in use")
        void failDeleteIssueField_If_InUse() {
            // given
            ProjectIdentifier pid = ProjectIdentifier.of("WORKSPACE", "PROJ");
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            Long issueFieldId = 10L;

            ProjectMember actor = mock(ProjectMember.class);
            IssueField issueField = mock(IssueField.class);

            given(projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFieldFinder.getWithProjectAndIssueTypeBy(
                            pid.workspaceKey(), pid.projectKey(), issueTypeId, issueFieldId))
                    .willReturn(issueField);

            willThrow(new BadRequestException(ISSUE_FIELD_IN_USE))
                    .given(issueFieldValidator)
                    .ensureDeletable(issueField);

            // when & then
            assertThatThrownBy(() -> sut.delete(pid, issueTypeId, issueFieldId, actorMemberId))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
