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
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SystemRoleAuthorizationService;
import com.tissue.feature.member.domain.Member;
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
    private MemberFinder memberFinder;

    @Mock
    private IssueFieldRepository issueFieldRepository;

    @Mock
    private IssueFieldValidator issueFieldValidator;

    @Mock
    private IssuePolicy issuePolicy;

    @Mock
    private SystemRoleAuthorizationService systemRoleAuthorizationService;

    @InjectMocks
    private IssueFieldService sut;

    @Nested
    @DisplayName("add issue field")
    class AddIssueField {

        @ParameterizedTest
        @EnumSource(
                value = IssueFieldType.class,
                names = {"SELECT_OPTION", "CHECKLIST"},
                mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("success: add issue field without options")
        void successAddIssueField(IssueFieldType fieldType) {
            // given
            Long actorMemberId = 1L;
            Long issueTypeId = 1L;
            Name fieldName = Name.of("goal");

            CreateIssueFieldCommand cmd = CreateIssueFieldCommand.builder()
                    .name(fieldName)
                    .issueFieldType(fieldType)
                    .required(false)
                    .position(0)
                    .build();

            Member actor = mock(Member.class);
            IssueType issueType = mock(IssueType.class);
            IssueField issueField = mock(IssueField.class);

            given(issueTypeFinder.getById(issueTypeId)).willReturn(issueType);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(issueType.addField(fieldName, cmd.description(), fieldType, cmd.required(), cmd.position()))
                    .willReturn(issueField);
            given(issueField.getIssueFieldType()).willReturn(fieldType);
            given(issueFieldRepository.save(any(IssueField.class))).willReturn(issueField);

            // when
            sut.addField(issueTypeId, cmd, actorMemberId);

            // then
            then(systemRoleAuthorizationService).should().requireSystemAdmin(actor);
            then(issueFieldValidator).should().ensureUniqueLabel(issueType, fieldName);
            then(issueType).should().addField(fieldName, cmd.description(), fieldType, cmd.required(), cmd.position());
            then(issueFieldRepository).should().save(any(IssueField.class));
            then(issuePolicy).shouldHaveNoInteractions();
        }

        @ParameterizedTest
        @EnumSource(
                value = IssueFieldType.class,
                names = {"SELECT_OPTION", "CHECKLIST"})
        @DisplayName("success: add issue field with initial options")
        void successAddIssueFieldWithOptions(IssueFieldType fieldType) {
            // given
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

            Member actor = mock(Member.class);
            IssueType issueType = mock(IssueType.class);
            IssueField issueField = mock(IssueField.class);

            given(issueTypeFinder.getById(issueTypeId)).willReturn(issueType);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(issueType.addField(fieldName, cmd.description(), fieldType, cmd.required(), cmd.position()))
                    .willReturn(issueField);
            given(issueField.getIssueFieldType()).willReturn(fieldType);
            given(issueFieldRepository.save(any(IssueField.class))).willReturn(issueField);

            // when
            sut.addField(issueTypeId, cmd, actorMemberId);

            // then
            then(systemRoleAuthorizationService).should().requireSystemAdmin(actor);
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
            Long actorMemberId = 1L;
            Long issueFieldId = 10L;

            Member actor = mock(Member.class);
            IssueField issueField = mock(IssueField.class);

            given(issueFieldFinder.getWithIssueType(issueFieldId)).willReturn(issueField);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);

            // when
            sut.delete(issueFieldId, actorMemberId);

            // then
            then(systemRoleAuthorizationService).should().requireSystemAdmin(actor);
            then(issueFieldValidator).should().ensureDeletable(issueField);
            then(issueFieldRepository).should().delete(issueField);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if issue field is in use")
        void failDeleteIssueField_If_InUse() {
            // given
            Long actorMemberId = 1L;
            Long issueFieldId = 10L;

            Member actor = mock(Member.class);
            IssueField issueField = mock(IssueField.class);

            given(issueFieldFinder.getWithIssueType(issueFieldId)).willReturn(issueField);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);

            willThrow(new BadRequestException(ISSUE_FIELD_IN_USE))
                    .given(issueFieldValidator)
                    .ensureDeletable(issueField);

            // when & then
            assertThatThrownBy(() -> sut.delete(issueFieldId, actorMemberId)).isInstanceOf(BadRequestException.class);
        }
    }
}
