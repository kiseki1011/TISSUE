package com.tissue.feature.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentCommandRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.service.WikiCommandService;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
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
class WikiCommandServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WikiCommandService sut;

    @Autowired
    private WikiDocumentCommandRepository wikiDocumentCommandRepository;

    @Autowired
    private WikiDocumentQueryRepository wikiDocumentQueryRepository;

    @Autowired
    private WikiSnapshotRepository wikiSnapshotRepository;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    private static final String WORKSPACE_KEY = "WORKSPACE";

    private Member owner;
    private Member regularMember;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = memberCommandRepository.save(Member.create("owner@trytissue.dev", "owner", "Gildong Hong"));
        regularMember = memberCommandRepository.save(Member.create("member@trytissue.dev", "member", "John Doe"));
        workspace = workspaceRepository.save(Workspace.create(WORKSPACE_KEY, "Workspace", null));
        workspaceMemberCommandRepository.save(WorkspaceMember.create(owner, workspace, WorkspaceRole.OWNER));
        workspaceMemberCommandRepository.save(WorkspaceMember.create(regularMember, workspace, WorkspaceRole.MEMBER));
        setSecurityContext(owner);
        em.flush();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("create document")
    class CreateDocument {

        @Test
        @DisplayName("success: create document without parent")
        void successCreateWithoutParent() {
            // given
            DocumentCreateCommand cmd = new DocumentCreateCommand("New Doc", "content", null);

            // when
            DocumentResponse response = sut.create(WORKSPACE_KEY, cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument doc = wikiDocumentQueryRepository
                    .findByIdAndWorkspaceKey(response.id(), WORKSPACE_KEY)
                    .orElseThrow();

            assertThat(doc.getTitle()).isEqualTo("New Doc");
            assertThat(doc.getContent()).isEqualTo("content");
            assertThat(doc.getParentDocument()).isNull();
        }

        @Test
        @DisplayName("success: create document with parent")
        void successCreateWithParent() {
            // given
            WikiDocument parent = saveDocument("Parent Doc", "content");
            em.flush();
            em.clear();

            DocumentCreateCommand cmd = new DocumentCreateCommand("Child Doc", "content", parent.getId());

            // when
            DocumentResponse response = sut.create(WORKSPACE_KEY, cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument child = wikiDocumentQueryRepository
                    .findWithParentByWorkspaceKeyAndId(WORKSPACE_KEY, response.id())
                    .orElseThrow();

            assertThat(child.getTitle()).isEqualTo("Child Doc");
            assertThat(child.getParentDocument()).isNotNull();
            assertThat(child.getParentDocument().getId()).isEqualTo(parent.getId());
        }
    }

    @Nested
    @DisplayName("update title")
    class UpdateTitle {

        @Test
        @DisplayName("success: update document title")
        void successUpdateTitle() {
            // given
            WikiDocument doc = saveDocument("Original", "content");
            em.flush();
            em.clear();

            // when
            sut.updateTitle(WORKSPACE_KEY, doc.getId(), "New Title", owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument updated = wikiDocumentQueryRepository
                    .findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY)
                    .orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("fail: locked document rejects title update")
        void failUpdateTitle_If_Locked() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            doc.lock();
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> sut.updateTitle(WORKSPACE_KEY, doc.getId(), "New Title", owner.getId()))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("update content")
    class UpdateContent {

        @Test
        @DisplayName("success: update content and create snapshot with bumped version")
        void successUpdateContent() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            em.clear();

            UpdateDocumentContentCommand cmd =
                    new UpdateDocumentContentCommand("updated content", SemanticUpdateType.MINOR, "minor edit");

            // when
            sut.updateContent(WORKSPACE_KEY, doc.getId(), cmd, owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument updated = wikiDocumentQueryRepository
                    .findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY)
                    .orElseThrow();
            assertThat(updated.getContent()).isEqualTo("updated content");
            assertThat(updated.getCurrentSnapshotVersion().toString()).isEqualTo("1.1.0");

            List<WikiDocumentSnapshot> snapshots =
                    wikiSnapshotRepository.findByDocumentIdOrderByVersionDesc(doc.getId(), WORKSPACE_KEY);
            assertThat(snapshots).hasSize(1);
            assertThat(snapshots.getFirst().getSnapshotContent()).isEqualTo("updated content");
        }
    }

    @Nested
    @DisplayName("set parent")
    class SetParent {

        @Test
        @DisplayName("success: set parent document")
        void successSetParent() {
            // given
            WikiDocument parent = saveDocument("Parent Doc", "content");
            WikiDocument child = saveDocument("Child Doc", "content");
            em.flush();
            em.clear();

            // when
            sut.setParent(WORKSPACE_KEY, child.getId(), parent.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument updated = wikiDocumentQueryRepository
                    .findWithParentByWorkspaceKeyAndId(WORKSPACE_KEY, child.getId())
                    .orElseThrow();
            assertThat(updated.getParentDocument()).isNotNull();
            assertThat(updated.getParentDocument().getId()).isEqualTo(parent.getId());
        }

        @Test
        @DisplayName("success: detach parent document")
        void successDetachParent() {
            // given
            WikiDocument parent = saveDocument("Parent Doc", "content");
            WikiDocument child = saveDocument("Child Doc", "content", parent);
            em.flush();
            em.clear();

            // when
            sut.setParent(WORKSPACE_KEY, child.getId(), null, owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument updated = wikiDocumentQueryRepository
                    .findWithParentByWorkspaceKeyAndId(WORKSPACE_KEY, child.getId())
                    .orElseThrow();
            assertThat(updated.getParentDocument()).isNull();
        }
    }

    @Nested
    @DisplayName("lock document")
    class LockDocument {

        @Test
        @DisplayName("success: lock document")
        void successLock() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            em.clear();

            // when
            sut.lock(WORKSPACE_KEY, doc.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument locked = wikiDocumentQueryRepository
                    .findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY)
                    .orElseThrow();
            assertThat(locked.isLocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("unlock document")
    class UnlockDocument {

        @Test
        @DisplayName("success: unlock document")
        void successUnlock() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            doc.lock();
            em.flush();
            em.clear();

            // when
            sut.unLock(WORKSPACE_KEY, doc.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument unlocked = wikiDocumentQueryRepository
                    .findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY)
                    .orElseThrow();
            assertThat(unlocked.isLocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("fail: MEMBER cannot lock document (unless author)")
        void failLock_If_NotCreatorAndMember() {
            // given
            setSecurityContext(owner);
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> sut.lock(WORKSPACE_KEY, doc.getId(), regularMember.getId()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("fail: MEMBER cannot delete document (unless author)")
        void failDelete_If_NotCreatorAndMember() {
            // given
            setSecurityContext(owner);
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> sut.delete(WORKSPACE_KEY, doc.getId(), regularMember.getId()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("success: document author (MEMBER) can lock own document")
        void successCreatorCanLock() {
            // given
            setSecurityContext(regularMember);
            WikiDocument doc = saveDocument("My Doc", "content");
            em.flush();
            em.clear();

            // when
            sut.lock(WORKSPACE_KEY, doc.getId(), regularMember.getId());
            em.flush();
            em.clear();

            // then
            WikiDocument locked = wikiDocumentQueryRepository
                    .findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY)
                    .orElseThrow();
            assertThat(locked.isLocked()).isTrue();
        }

        @Test
        @DisplayName("success: document author (MEMBER) can delete own document")
        void successCreatorCanDelete() {
            // given
            setSecurityContext(regularMember);
            WikiDocument doc = saveDocument("My Doc", "content");
            em.flush();
            em.clear();

            // when
            sut.delete(WORKSPACE_KEY, doc.getId(), regularMember.getId());
            em.flush();
            em.clear();

            // then
            assertThat(wikiDocumentQueryRepository.findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("delete document")
    class DeleteDocument {

        @Test
        @DisplayName("success: cannot retrieve soft-deleted document with normal query")
        void successSoftDelete() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            em.flush();
            em.clear();

            // when
            sut.delete(WORKSPACE_KEY, doc.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            assertThat(wikiDocumentQueryRepository.findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY))
                    .isEmpty();
            assertThat(wikiDocumentQueryRepository.findDeletedByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("restore document")
    class RestoreDocument {

        @Test
        @DisplayName("success: restore soft-deleted document")
        void successRestore() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            doc.softDelete();
            em.flush();
            em.clear();

            // when
            sut.restore(WORKSPACE_KEY, doc.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            assertThat(wikiDocumentQueryRepository.findByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("hard delete document")
    class HardDeleteDocument {

        @Test
        @DisplayName("success: permanently remove soft-deleted document")
        void successHardDelete() {
            // given
            WikiDocument doc = saveDocument("Doc", "content");
            doc.softDelete();
            em.flush();
            em.clear();

            // when
            sut.hardDelete(WORKSPACE_KEY, doc.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            assertThat(wikiDocumentQueryRepository.findDeletedByIdAndWorkspaceKey(doc.getId(), WORKSPACE_KEY))
                    .isEmpty();
        }

        @Test
        @DisplayName("fail: hard delete fails when document has children")
        void failHardDelete_If_HasChildren() {
            // given
            WikiDocument parent = saveDocument("Parent Doc", "content");
            saveDocument("Child Doc", "content", parent);
            parent.softDelete();
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> sut.hardDelete(WORKSPACE_KEY, parent.getId(), owner.getId()))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    private WikiDocument saveDocument(String title, String content) {
        return wikiDocumentCommandRepository.save(WikiDocument.create(workspace, title, content, null));
    }

    private WikiDocument saveDocument(String title, String content, WikiDocument parent) {
        return wikiDocumentCommandRepository.save(WikiDocument.create(workspace, title, content, parent));
    }

    private void setSecurityContext(Member member) {
        MemberDetails details = new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
