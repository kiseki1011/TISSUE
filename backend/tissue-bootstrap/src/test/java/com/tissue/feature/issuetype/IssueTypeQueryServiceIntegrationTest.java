package com.tissue.feature.issuetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.dto.response.IssueTypeSummary;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.application.service.IssueTypeQueryService;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeNotFoundException;
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
class IssueTypeQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueTypeQueryService sut;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    private Member gildong;
    private Workflow workflow;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));

        workflow = Workflow.create(Name.of("Default Workflow"), null, ColorType.YELLOW);
        workflow.addState(Name.of("Open"), null, ColorType.GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("Done"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        em.flush();
        em.clear();
    }

    private IssueType saveIssueType(String name) {
        Workflow managedWorkflow = em.find(Workflow.class, workflow.getId());
        IssueType issueType = IssueType.create(
                Name.of(name), "desc", ColorType.RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, managedWorkflow);
        return issueTypeRepository.save(issueType);
    }

    @Nested
    @DisplayName("get issue types")
    class GetIssueTypes {

        @Test
        @DisplayName("returns every issue type")
        void returnsAllIssueTypes() {
            // given
            saveIssueType("Bug");
            saveIssueType("Story");
            em.flush();
            em.clear();

            // when
            List<IssueTypeSummary> result = sut.getIssueTypes(gildong.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(IssueTypeSummary::name).containsExactlyInAnyOrder("Bug", "Story");
            assertThat(result).allMatch(summary -> summary.workflowId().equals(workflow.getId()));
            assertThat(result).allMatch(summary -> "Default Workflow".equals(summary.workflowName()));
        }

        @Test
        @DisplayName("returns an empty list when there are no issue types")
        void returnsEmptyListWhenNoIssueTypes() {
            // when
            List<IssueTypeSummary> result = sut.getIssueTypes(gildong.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("get issue type detail")
    class GetIssueTypeDetail {

        @Test
        @DisplayName("returns the issue type with its ordered fields and options")
        void returnsDetailWithFieldsAndOptions() {
            // given
            IssueType issueType = saveIssueType("Bug");
            IssueField statusField =
                    issueType.addField(Name.of("Severity"), "severity level", IssueFieldType.SELECT_OPTION, true, 0);
            statusField.addOption(Name.of("Low"));
            statusField.addOption(Name.of("High"));
            issueType.addField(Name.of("Note"), "free text", IssueFieldType.TEXT, false, 1);
            em.flush();
            em.clear();

            // when
            IssueTypeDetail detail = sut.getIssueTypeDetail(issueType.getId(), gildong.getId());

            // then
            assertThat(detail.id()).isEqualTo(issueType.getId());
            assertThat(detail.name()).isEqualTo("Bug");
            assertThat(detail.workflowId()).isEqualTo(workflow.getId());
            assertThat(detail.workflowName()).isEqualTo("Default Workflow");
            assertThat(detail.fields()).hasSize(2);
            assertThat(detail.fields().getFirst().name()).isEqualTo("Severity");
            assertThat(detail.fields().getFirst().type()).isEqualTo(IssueFieldType.SELECT_OPTION);
            assertThat(detail.fields().getFirst().options()).extracting("name").containsExactly("Low", "High");
            assertThat(detail.fields().get(1).name()).isEqualTo("Note");
            assertThat(detail.fields().get(1).options()).isEmpty();
        }

        @Test
        @DisplayName("returns the issue type with an empty fields list when no fields exist")
        void returnsDetailWithEmptyFields() {
            // given
            IssueType issueType = saveIssueType("Bug");
            em.flush();
            em.clear();

            // when
            IssueTypeDetail detail = sut.getIssueTypeDetail(issueType.getId(), gildong.getId());

            // then
            assertThat(detail.fields()).isEmpty();
        }

        @Test
        @DisplayName("throws when the issue type does not exist")
        void throwsWhenNotFound() {
            // when & then
            assertThatThrownBy(() -> sut.getIssueTypeDetail(999L, gildong.getId()))
                    .isInstanceOf(IssueTypeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("get issue type details")
    class GetIssueTypeDetails {

        @Test
        @DisplayName("returns every issue type with its own fields, without mixing fields across types")
        void returnsAllTypesWithTheirOwnFields() {
            // given
            IssueType bug = saveIssueType("Bug");
            IssueField severity =
                    bug.addField(Name.of("Severity"), "severity level", IssueFieldType.SELECT_OPTION, true, 0);
            severity.addOption(Name.of("Low"));
            severity.addOption(Name.of("High"));

            IssueType story = saveIssueType("Story");
            story.addField(Name.of("Acceptance"), "acceptance criteria", IssueFieldType.TEXT, false, 0);

            saveIssueType("Task");
            em.flush();
            em.clear();

            // when
            List<IssueTypeDetail> result = sut.getIssueTypeDetails(gildong.getId());

            // then
            assertThat(result).hasSize(3);

            IssueTypeDetail bugDetail = result.stream()
                    .filter(detail -> detail.name().equals("Bug"))
                    .findFirst()
                    .orElseThrow();
            assertThat(bugDetail.fields()).extracting("name").containsExactly("Severity");
            assertThat(bugDetail.fields().getFirst().options())
                    .extracting("name")
                    .containsExactly("Low", "High");

            IssueTypeDetail storyDetail = result.stream()
                    .filter(detail -> detail.name().equals("Story"))
                    .findFirst()
                    .orElseThrow();
            assertThat(storyDetail.fields()).extracting("name").containsExactly("Acceptance");

            IssueTypeDetail taskDetail = result.stream()
                    .filter(detail -> detail.name().equals("Task"))
                    .findFirst()
                    .orElseThrow();
            assertThat(taskDetail.fields()).isEmpty();
        }

        @Test
        @DisplayName("returns an empty list when there are no issue types")
        void returnsEmptyListWhenNoIssueTypes() {
            // when & then
            assertThat(sut.getIssueTypeDetails(gildong.getId())).isEmpty();
        }
    }
}
