package com.tissue.feature.issue.application.service.validator;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.CUSTOM_FIELD_REQUIRED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.service.handler.IssueFieldTypeHandlerRegistry;
import com.tissue.feature.issuetype.application.port.repository.FieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

@ExtendWith(MockitoExtension.class)
class CustomFieldSchemaProcessorTest {

    @Mock
    private IssueFieldRepository issueFieldRepo;

    @Mock
    private FieldOptionRepository enumOptionRepo;

    @Mock
    private IssueFieldTypeHandlerRegistry fieldTypeHandler;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private CustomFieldSchemaProcessor sut;

    @Nested
    @DisplayName("validate and assign value")
    class ValidateAndAssign {

        @Test
        @DisplayName("success: parses and sets custom field value on issue")
        void successCustomFieldValueValidationAndAssign() {
            // given
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);
            IssueField field = mock(IssueField.class);

            given(issue.getIssueType()).willReturn(issueType);
            given(issueFieldRepo.findByIssueType(issueType)).willReturn(List.of(field));
            given(field.getId()).willReturn(1L);
            given(field.getIssueFieldType()).willReturn(IssueFieldType.TEXT);

            Map<Long, Object> rawInput = new HashMap<>(Map.of(1L, "hello"));

            given(fieldTypeHandler.isBlank(field, "hello")).willReturn(false);
            given(fieldTypeHandler.parse(field, "hello")).willReturn("hello");
            given(fieldTypeHandler.toJsonValue(field, "hello")).willReturn("hello");

            // when
            sut.validateAndAssign(rawInput, issue);

            // then
            then(issue).should().setCustomFieldValue("1", "hello");
        }

        @Test
        @DisplayName("success: clears custom field value if empty value for optional field")
        void successClearCustomFieldValue() {
            // given
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);
            IssueField field = mock(IssueField.class);

            given(issue.getIssueType()).willReturn(issueType);
            given(issueFieldRepo.findByIssueType(issueType)).willReturn(List.of(field));
            given(field.getId()).willReturn(1L);
            given(field.getIssueFieldType()).willReturn(IssueFieldType.TEXT);

            Map<Long, Object> rawInput = new HashMap<>(Map.of(1L, ""));

            given(field.isRequired()).willReturn(false);
            given(fieldTypeHandler.isBlank(field, "")).willReturn(true);

            // when
            sut.validateAndAssign(rawInput, issue);

            // then
            then(issue).should().clearCustomField("1");
            then(issue).should(never()).setCustomFieldValue("1", "");
        }

        @Test
        @DisplayName("fail: throws BadRequestException if empty value for required field")
        void failCustomFieldValueValidation_If_RequiredFieldEmpty() {
            // given
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);
            IssueField field = mock(IssueField.class);

            given(issue.getIssueType()).willReturn(issueType);
            given(issueFieldRepo.findByIssueType(issueType)).willReturn(List.of(field));
            given(field.getId()).willReturn(1L);
            given(field.getIssueFieldType()).willReturn(IssueFieldType.TEXT);

            Map<Long, Object> rawInput = new HashMap<>(Map.of(1L, ""));

            given(field.isRequired()).willReturn(true);
            given(fieldTypeHandler.isBlank(field, "")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.validateAndAssign(rawInput, issue))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(CUSTOM_FIELD_REQUIRED);
        }
    }

    @Nested
    @DisplayName("validate and apply patch")
    class ValidateAndPatch {

        @Test
        @DisplayName("success: parses and sets value for known field ID")
        void successCustomFieldValueValidationAndPatch() {
            // given
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);
            IssueField field = mock(IssueField.class);

            given(issue.getIssueType()).willReturn(issueType);
            given(issueFieldRepo.findByIssueType(issueType)).willReturn(List.of(field));
            given(field.getId()).willReturn(1L);
            given(field.getIssueFieldType()).willReturn(IssueFieldType.INTEGER);

            Map<Long, Object> rawInput = new HashMap<>(Map.of(1L, 42));

            given(fieldTypeHandler.isBlank(field, 42)).willReturn(false);
            given(fieldTypeHandler.parse(field, 42)).willReturn(42);
            given(fieldTypeHandler.toJsonValue(field, 42)).willReturn(42);

            // when
            sut.validateAndApplyPatch(rawInput, issue);

            // then
            then(issue).should().setCustomFieldValue("1", 42);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if field ID does not exist in schema")
        void failValidationAndPatch_If_FieldIdNotExist() {
            // given
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);

            given(issue.getIssueType()).willReturn(issueType);
            given(issueFieldRepo.findByIssueType(issueType)).willReturn(List.of());

            Map<Long, Object> rawInput = new HashMap<>(Map.of(999L, "value"));

            // when & then
            assertThatThrownBy(() -> sut.validateAndApplyPatch(rawInput, issue))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(UNKNOWN_CUSTOM_FIELD_ID);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if empty value for required field")
        void failValidationAndPatch_If_RequiredFieldEmpty() {
            // given
            Issue issue = mock(Issue.class);
            IssueType issueType = mock(IssueType.class);
            IssueField field = mock(IssueField.class);

            given(issue.getIssueType()).willReturn(issueType);
            given(issueFieldRepo.findByIssueType(issueType)).willReturn(List.of(field));
            given(field.getId()).willReturn(1L);
            given(field.getIssueFieldType()).willReturn(IssueFieldType.TEXT);

            Map<Long, Object> rawInput = new HashMap<>(Map.of(1L, ""));

            given(field.isRequired()).willReturn(true);
            given(fieldTypeHandler.isBlank(field, "")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.validateAndApplyPatch(rawInput, issue))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(CUSTOM_FIELD_REQUIRED);
        }
    }
}
