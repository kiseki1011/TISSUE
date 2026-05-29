package com.tissue.feature.issuetype;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.request.CreateIssueFieldCommand;
import com.tissue.feature.issuetype.application.dto.response.IssueFieldResponse;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.service.IssueFieldService;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueFieldServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueFieldService issueFieldService;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private IssueFieldRepository issueFieldRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    private Member admin;
    private Long issueTypeId;

    @BeforeEach
    void setUp() {
        admin = memberRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "HongGilDong"));

        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.YELLOW);
        workflow.addState(Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Bug"), null, ColorType.RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(issueType);
        issueTypeId = issueType.getId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("add field")
    class AddField {

        @Test
        @DisplayName("adds 'SELECT_OPTION' field with initial options")
        void addSelectOptionFieldWithOptions() {
            // given
            CreateIssueFieldCommand cmd = CreateIssueFieldCommand.builder()
                    .name(Name.of("Priority"))
                    .description("Issue priority")
                    .issueFieldType(IssueFieldType.SELECT_OPTION)
                    .required(true)
                    .initialOptions(List.of(Name.of("Low"), Name.of("Medium"), Name.of("High")))
                    .position(0)
                    .build();

            // when
            IssueFieldResponse response = issueFieldService.addField(issueTypeId, cmd, admin.getId());
            em.flush();
            em.clear();

            // then
            IssueField field = issueFieldRepository
                    .findWithIssueTypeById(response.issueFieldId())
                    .orElseThrow();

            assertThat(field.getName()).isEqualTo("Priority");
            assertThat(field.getIssueFieldType()).isEqualTo(IssueFieldType.SELECT_OPTION);
            assertThat(field.isRequired()).isTrue();
            assertThat(field.getOptions()).hasSize(3);
            assertThat(field.getOptions())
                    .extracting(FieldOption::getName)
                    .containsExactlyInAnyOrder("Low", "Medium", "High");
        }

        @Test
        @DisplayName("multiple fields must be ordered by position")
        void fieldsOrderedByPosition() {
            // given
            issueFieldService.addField(
                    issueTypeId,
                    CreateIssueFieldCommand.builder()
                            .name(Name.of("Description"))
                            .issueFieldType(IssueFieldType.TEXT)
                            .required(false)
                            .initialOptions(List.of())
                            .position(2)
                            .build(),
                    admin.getId());

            issueFieldService.addField(
                    issueTypeId,
                    CreateIssueFieldCommand.builder()
                            .name(Name.of("Priority"))
                            .issueFieldType(IssueFieldType.SELECT_OPTION)
                            .required(true)
                            .initialOptions(List.of(Name.of("Low"), Name.of("High")))
                            .position(0)
                            .build(),
                    admin.getId());

            issueFieldService.addField(
                    issueTypeId,
                    CreateIssueFieldCommand.builder()
                            .name(Name.of("Due Date"))
                            .issueFieldType(IssueFieldType.DATE)
                            .required(false)
                            .initialOptions(List.of())
                            .position(1)
                            .build(),
                    admin.getId());

            em.flush();
            em.clear();

            // when
            IssueType reloaded = issueTypeRepository.findById(issueTypeId).orElseThrow();

            // then
            assertThat(reloaded.getFields()).hasSize(3);
            assertThat(reloaded.getFields())
                    .extracting(IssueField::getName)
                    .containsExactly("Priority", "Due Date", "Description");
        }
    }
}
