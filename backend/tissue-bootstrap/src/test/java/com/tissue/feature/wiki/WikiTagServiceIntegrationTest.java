package com.tissue.feature.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.wiki.application.dto.request.AttachWikiTagCommand;
import com.tissue.feature.wiki.application.dto.response.WikiTagDetail;
import com.tissue.feature.wiki.application.dto.response.WikiTagResponse;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentCommandRepository;
import com.tissue.feature.wiki.application.port.repository.WikiTagRepository;
import com.tissue.feature.wiki.application.service.WikiTagCommandService;
import com.tissue.feature.wiki.application.service.WikiTagQueryService;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.policy.WikiTagConstraintPolicy;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WikiTagServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WikiTagCommandService sut;

    @Autowired
    private WikiTagQueryService wikiTagQueryService;

    @Autowired
    private WikiTagRepository wikiTagRepository;

    @Autowired
    private WikiDocumentCommandRepository wikiDocumentCommandRepository;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    private Member actor;

    @BeforeEach
    void setUp() {
        actor = memberCommandRepository.save(Member.create("actor@trytissue.dev", "actor", "Actor"));
        setSecurityContext(actor);
        em.flush();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Member member) {
        MemberDetails details = new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("attach tag")
    class AttachTag {

        @Test
        @DisplayName("success: attaching an unknown name creates the tag (create if absent)")
        void successAttach_CreatesTag() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();

            // when
            WikiTagResponse response = sut.attachTag(doc.getId(), attach("architecture"), actor.getId());
            em.flush();
            em.clear();

            // then
            assertThat(wikiTagRepository.findById(response.tagId())).isPresent();
        }

        @Test
        @DisplayName("success: attaching an existing name reuses the same tag")
        void successAttach_ReusesExistingTag() {
            // given
            WikiDocument doc1 = saveDocument("Doc 1", "content");
            WikiDocument doc2 = saveDocument("Doc 2", "content");
            em.flush();

            // when
            Long tagId1 = sut.attachTag(doc1.getId(), attach("architecture"), actor.getId())
                    .tagId();
            Long tagId2 = sut.attachTag(doc2.getId(), attach("Architecture"), actor.getId())
                    .tagId();
            em.flush();
            em.clear();

            // then
            assertThat(tagId1).isEqualTo(tagId2);
            assertThat(wikiTagRepository.findAll(PageRequest.of(0, 10)).getTotalElements())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("success: attaching the same tag to the same document twice is idempotent")
        void successAttach_Idempotent() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();

            // when
            sut.attachTag(doc.getId(), attach("architecture"), actor.getId());
            sut.attachTag(doc.getId(), attach("architecture"), actor.getId());
            em.flush();
            em.clear();

            // then
            assertThat(wikiDocumentFinderById(doc.getId()).getTags()).hasSize(1);
        }

        @Test
        @DisplayName("fail: attaching tags beyond the limit throws ResourceConflictException")
        void failAttach_When_LimitExceeded() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            for (int i = 0; i < WikiTagConstraintPolicy.MAX_TAGS_PER_DOCUMENT; i++) {
                sut.attachTag(doc.getId(), attach("tag-" + i), actor.getId());
            }
            em.flush();

            // when & then
            assertThatThrownBy(() -> sut.attachTag(doc.getId(), attach("one-too-many"), actor.getId()))
                    .isInstanceOf(ResourceConflictException.class);
        }
    }

    @Nested
    @DisplayName("search tags by name")
    class SearchTags {

        @Test
        @DisplayName("success: substring match is case insensitive")
        void successSearch_ByName() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            sut.attachTag(doc.getId(), attach("Architecture"), actor.getId());
            sut.attachTag(doc.getId(), attach("Runbook"), actor.getId());
            em.flush();
            em.clear();

            // when
            List<WikiTagDetail> matches = wikiTagQueryService
                    .searchTags("arch", PageRequest.of(0, 10), actor.getId())
                    .getContent();

            // then
            assertThat(matches).extracting(WikiTagDetail::name).containsExactly("Architecture");
        }

        @Test
        @DisplayName("success: a substring in the middle of the name matches")
        void successSearch_MiddleSubstring() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            sut.attachTag(doc.getId(), attach("Architecture"), actor.getId());
            sut.attachTag(doc.getId(), attach("Runbook"), actor.getId());
            em.flush();
            em.clear();

            // when
            List<WikiTagDetail> matches = wikiTagQueryService
                    .searchTags("tect", PageRequest.of(0, 10), actor.getId())
                    .getContent();

            // then
            assertThat(matches).extracting(WikiTagDetail::name).containsExactly("Architecture");
        }

        @Test
        @DisplayName("success: blank keyword returns all tags")
        void successSearch_BlankReturnsAll() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            sut.attachTag(doc.getId(), attach("Architecture"), actor.getId());
            sut.attachTag(doc.getId(), attach("Runbook"), actor.getId());
            em.flush();
            em.clear();

            // when
            List<WikiTagDetail> matches = wikiTagQueryService
                    .searchTags(null, PageRequest.of(0, 10), actor.getId())
                    .getContent();

            // then
            assertThat(matches).hasSize(2);
        }
    }

    private AttachWikiTagCommand attach(String name) {
        return new AttachWikiTagCommand(Name.of(name), ColorType.BLUE);
    }

    private WikiDocument saveDocument(String title, String content) {
        return wikiDocumentCommandRepository.save(WikiDocument.create(title, content, null));
    }

    private WikiDocument wikiDocumentFinderById(Long id) {
        return em.find(WikiDocument.class, id);
    }
}
