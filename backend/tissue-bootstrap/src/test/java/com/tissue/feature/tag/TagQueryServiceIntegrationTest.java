package com.tissue.feature.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.tag.application.dto.request.CreateTagCommand;
import com.tissue.feature.tag.application.dto.response.TagDetail;
import com.tissue.feature.tag.application.service.TagCommandService;
import com.tissue.feature.tag.application.service.TagQueryService;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TagQueryServiceIntegrationTest extends IntegrationTestSupport {

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");
    private static final Pageable PAGE = PageRequest.of(0, 20);

    @Autowired
    private TagQueryService sut;

    @Autowired
    private TagCommandService tagCommandService;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "Tester"));
        Project project = projectRepository.save(Project.create("PROJ", "Test Project", null));
        projectMemberRepository.save(ProjectMember.createManager(project, member));
        em.flush();
        em.clear();

        createTag(PID, "Backend");
        createTag(PID, "Frontend");
        createTag(PID, "Database");
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("search tags")
    class SearchTags {

        @Test
        @DisplayName("success: keyword does a contains match on the tag name")
        void searchByKeyword() {
            // when
            Page<TagDetail> result = sut.searchTags(PID, "end", PAGE, member.getId());

            // then
            assertThat(result.getContent())
                    .extracting(TagDetail::name)
                    .containsExactlyInAnyOrder("Backend", "Frontend");
        }

        @Test
        @DisplayName("success: matching is case-insensitive")
        void searchCaseInsensitive() {
            // when
            Page<TagDetail> result = sut.searchTags(PID, "DATA", PAGE, member.getId());

            // then
            assertThat(result.getContent()).extracting(TagDetail::name).containsExactly("Database");
        }

        @Test
        @DisplayName("success: blank keyword returns all project tags")
        void blankReturnsAll() {
            // when
            Page<TagDetail> result = sut.searchTags(PID, null, PAGE, member.getId());

            // then
            assertThat(result.getContent())
                    .extracting(TagDetail::name)
                    .containsExactlyInAnyOrder("Backend", "Frontend", "Database");
        }

        @Test
        @DisplayName("success: results are scoped to the project")
        void scopedToProject() {
            // given
            ProjectIdentifier other = ProjectIdentifier.ofProjectKey("OTHER");
            Project otherProject = projectRepository.save(Project.create("OTHER", "Other Project", null));
            projectMemberRepository.save(ProjectMember.createManager(otherProject, member));
            em.flush();
            em.clear();
            createTag(other, "Backend");
            em.flush();
            em.clear();

            // when
            Page<TagDetail> result = sut.searchTags(PID, "backend", PAGE, member.getId());

            // then
            assertThat(result.getContent()).extracting(TagDetail::name).containsExactly("Backend");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    private void createTag(ProjectIdentifier pid, String name) {
        tagCommandService.create(
                pid,
                CreateTagCommand.builder()
                        .name(Name.of(name))
                        .description(null)
                        .color(ColorType.ANSI_BLUE)
                        .build(),
                member.getId());
    }
}
