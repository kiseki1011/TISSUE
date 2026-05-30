package com.tissue.feature.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentDetail;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSearchResult;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentSummary;
import com.tissue.feature.wiki.application.dto.response.WikiDocumentTreeNode;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotDetail;
import com.tissue.feature.wiki.application.dto.response.WikiSnapshotSummary;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentCommandRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.service.WikiQueryService;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.KeysetPageResponse;
import com.tissue.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WikiQueryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WikiQueryService sut;

    @Autowired
    private WikiDocumentCommandRepository wikiDocumentCommandRepository;

    @Autowired
    private WikiSnapshotRepository wikiSnapshotRepository;

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

    @Nested
    @DisplayName("get document detail")
    class GetDocumentDetail {

        @Test
        @DisplayName("success: returns document with parent info")
        void successReturnsDocumentWithParent() {
            // given
            WikiDocument parent = saveDocument("Parent Doc", "parent content");
            WikiDocument child = saveDocument("Child Doc", "child content", parent);
            em.flush();
            em.clear();

            // when
            WikiDocumentDetail detail = sut.getDocumentDetail(child.getId(), actor.getId());

            // then
            assertThat(detail.title()).isEqualTo("Child Doc");
            assertThat(detail.content()).isEqualTo("child content");
            assertThat(detail.parentDocumentId()).isEqualTo(parent.getId());
            assertThat(detail.parentDocumentTitle()).isEqualTo("Parent Doc");
        }
    }

    @Nested
    @DisplayName("get root documents")
    class GetRootDocuments {

        @Test
        @DisplayName("success: returns list of root documents summary")
        void successReturnsRootsWithHasChildren() {
            // given
            WikiDocument root1 = saveDocument("A Root", "content");
            saveDocument("B Root", "content");
            saveDocument("Child of A", "content", root1);
            em.flush();
            em.clear();

            // when
            List<WikiDocumentSummary> roots = sut.getRootDocuments(actor.getId());

            // then
            assertThat(roots).hasSize(2);
            assertThat(roots.get(0).title()).isEqualTo("A Root");
            assertThat(roots.get(0).hasChildren()).isTrue();
            assertThat(roots.get(1).title()).isEqualTo("B Root");
            assertThat(roots.get(1).hasChildren()).isFalse();
        }
    }

    @Nested
    @DisplayName("get children documents")
    class GetChildrenDocuments {

        @Test
        @DisplayName("success: returns children of parent document")
        void successReturnsChildren() {
            // given
            WikiDocument parent = saveDocument("Parent", "content");
            saveDocument("Child A", "content", parent);
            saveDocument("Child B", "content", parent);
            em.flush();
            em.clear();

            // when
            List<WikiDocumentSummary> children = sut.getChildrenDocuments(parent.getId(), actor.getId());

            // then
            assertThat(children).hasSize(2);
        }
    }

    @Nested
    @DisplayName("get document tree")
    class GetDocumentTree {

        @Test
        @DisplayName("success: returns flat tree with parent references")
        void successReturnsFlatTree() {
            // given
            WikiDocument root = saveDocument("Root", "content");
            WikiDocument child = saveDocument("Child", "content", root);
            em.flush();
            em.clear();

            // when
            List<WikiDocumentTreeNode> tree = sut.getDocumentTree(actor.getId());

            // then
            assertThat(tree).hasSize(2);

            WikiDocumentTreeNode childNode = tree.stream()
                    .filter(n -> n.id().equals(child.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(childNode.parentDocumentId()).isEqualTo(root.getId());

            WikiDocumentTreeNode rootNode = tree.stream()
                    .filter(n -> n.id().equals(root.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(rootNode.parentDocumentId()).isNull();
        }
    }

    @Nested
    @DisplayName("version history")
    class VersionHistory {

        @Test
        @DisplayName("success: returns snapshots in version desc order")
        void successReturnsSnapshotsDescending() {
            // given
            WikiDocument doc = saveDocument("Doc", "v1.0.0 content");
            doc.updateContent("v1.1.0 content", SemanticUpdateType.MINOR);
            wikiSnapshotRepository.save(
                    WikiDocumentSnapshot.create(doc, SemanticUpdateType.MINOR, "v1.0.0 edit reason"));
            doc.updateContent("v2.0.0 content", SemanticUpdateType.MAJOR);
            wikiSnapshotRepository.save(
                    WikiDocumentSnapshot.create(doc, SemanticUpdateType.MAJOR, "v1.1.0 edit reason"));
            em.flush();
            em.clear();

            // when
            List<WikiSnapshotSummary> history = sut.getVersionHistory(doc.getId(), actor.getId());

            // then
            assertThat(history).hasSize(2);
            assertThat(history.get(0).snapshotVersion()).isEqualTo("2.0.0");
            assertThat(history.get(0).editReason()).isEqualTo("v1.1.0 edit reason");
            assertThat(history.get(1).snapshotVersion()).isEqualTo("1.1.0");
        }

        @Test
        @DisplayName("success: returns snapshot detail with content")
        void successReturnsSnapshotDetail() {
            // given
            WikiDocument doc = saveDocument("Doc", "v1.0.0 content");
            doc.updateContent("v1.1.0 content", SemanticUpdateType.MINOR);
            WikiDocumentSnapshot snapshot = wikiSnapshotRepository.save(
                    WikiDocumentSnapshot.create(doc, SemanticUpdateType.MINOR, "edit reason"));
            em.flush();
            em.clear();

            // when
            WikiSnapshotDetail detail = sut.getVersionSnapshotDetail(doc.getId(), snapshot.getId(), actor.getId());

            // then
            assertThat(detail.snapshotContent()).isEqualTo("v1.1.0 content");
            assertThat(detail.snapshotVersion()).isEqualTo("1.1.0");
        }
    }

    @Nested
    @DisplayName("search documents")
    class SearchDocuments {

        @Test
        @DisplayName("success: search matches title and content (not case sensitive)")
        void successSearchByKeyword() {
            // given
            saveDocument("Title Keyword", "content");
            saveDocument("Title 2", "content");
            saveDocument("Title 3", "content");
            em.flush();
            em.clear();

            // when
            KeysetPageResponse<WikiDocumentSearchResult> result =
                    sut.searchDocuments("keyword", actor.getId(), null, null, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().title()).isEqualTo("Title Keyword");
        }

        @Test
        @DisplayName("success: search matches content keyword")
        void successSearchByContentKeyword() {
            // given
            saveDocument("Title 1", "content");
            saveDocument("Title 2", "content keyword content");
            saveDocument("Title 3", "content keyword content");
            em.flush();
            em.clear();

            // when
            KeysetPageResponse<WikiDocumentSearchResult> result =
                    sut.searchDocuments("keyword", actor.getId(), null, null, 20);

            // then
            assertThat(result.content()).hasSize(2);
        }

        @Test
        @DisplayName("success: keyset pagination returns distinct pages")
        void successKeysetPagination() {
            // given
            for (int i = 1; i <= 5; i++) {
                saveDocument("Doc " + i, "content keyword content");
            }
            em.flush();
            em.clear();

            // when
            KeysetPageResponse<WikiDocumentSearchResult> page1 =
                    sut.searchDocuments("keyword", actor.getId(), null, null, 3);
            KeysetPageResponse<WikiDocumentSearchResult> page2 = sut.searchDocuments(
                    "keyword", actor.getId(), page1.nextKeysetModifiedAt(), page1.nextKeysetId(), 3);

            // then
            assertThat(page1.content()).hasSize(3);
            assertThat(page2.content()).hasSize(2);

            List<Long> page1Ids =
                    page1.content().stream().map(WikiDocumentSearchResult::id).toList();
            List<Long> page2Ids =
                    page2.content().stream().map(WikiDocumentSearchResult::id).toList();
            assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
        }
    }

    private WikiDocument saveDocument(String title, String content) {
        return wikiDocumentCommandRepository.save(WikiDocument.create(title, content, null));
    }

    private WikiDocument saveDocument(String title, String content, WikiDocument parent) {
        return wikiDocumentCommandRepository.save(WikiDocument.create(title, content, parent));
    }

    private void setSecurityContext(Member member) {
        MemberDetails details = new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
