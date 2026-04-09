package com.tissue.feature.wiki.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.wiki.application.dto.request.DocumentCreateCommand;
import com.tissue.feature.wiki.application.dto.request.UpdateDocumentContentCommand;
import com.tissue.feature.wiki.application.dto.response.DocumentResponse;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentCommandRepository;
import com.tissue.feature.wiki.application.port.repository.WikiDocumentQueryRepository;
import com.tissue.feature.wiki.application.port.repository.WikiLinkRepository;
import com.tissue.feature.wiki.application.port.repository.WikiSnapshotRepository;
import com.tissue.feature.wiki.application.service.authorization.WikiAuthorizationService;
import com.tissue.feature.wiki.application.service.finder.WikiDocumentFinder;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.WikiDocumentSnapshot;
import com.tissue.feature.wiki.domain.WikiLink;
import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WikiCommandServiceTest {

    @Mock
    private WikiDocumentCommandRepository wikiDocumentCommandRepository;

    @Mock
    private WikiSnapshotRepository wikiSnapshotRepository;

    @Mock
    private WikiLinkRepository wikiLinkRepository;

    @Mock
    private WikiDocumentQueryRepository wikiDocumentQueryRepository;

    @Mock
    private WorkspaceMemberFinder workspaceMemberFinder;

    @Mock
    private WikiDocumentFinder wikiDocumentFinder;

    @Mock
    private WikiLinkTargetResolver wikiLinkTargetResolver;

    @Mock
    private WikiAuthorizationService wikiAuthorizationService;

    @InjectMocks
    private WikiCommandService sut;

    @Nested
    @DisplayName("create document")
    class CreateDocument {

        @Test
        @DisplayName("success: create document without parent")
        void successCreateDocumentWithoutParent() {
            // given
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            Workspace workspace = mock(Workspace.class);

            DocumentCreateCommand cmd = new DocumentCreateCommand("title", "content", null);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(actor.getWorkspace()).willReturn(workspace);
            given(workspace.getKey()).willReturn(workspaceKey);

            // when
            DocumentResponse response = sut.create(workspaceKey, cmd, actorMemberId);

            // then
            then(wikiDocumentCommandRepository).should().save(any(WikiDocument.class));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("success: create document with parent")
        void successCreateDocumentWithParent() {
            // given
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            Long parentDocumentId = 123L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            Workspace workspace = mock(Workspace.class);
            WikiDocument parentDocument = mock(WikiDocument.class);

            DocumentCreateCommand cmd = new DocumentCreateCommand("title", "content", parentDocumentId);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(actor.getWorkspace()).willReturn(workspace);
            given(workspace.getKey()).willReturn(workspaceKey);
            given(wikiDocumentFinder.getBy(workspaceKey, parentDocumentId)).willReturn(parentDocument);
            given(parentDocument.getWorkspaceKey()).willReturn(workspaceKey);

            // when
            DocumentResponse response = sut.create(workspaceKey, cmd, actorMemberId);

            // then
            then(wikiDocumentCommandRepository).should().save(any(WikiDocument.class));
            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("update title")
    class UpdateTitle {

        @Test
        @DisplayName("success: update document title")
        void successUpdateTitle() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(mock(WorkspaceMember.class));
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(document);

            // when
            sut.updateTitle(workspaceKey, wikiId, "new title", actorMemberId);

            // then
            then(document).should().updateTitle("new title");
        }
    }

    @Nested
    @DisplayName("update content")
    class UpdateContent {

        @Test
        @DisplayName("success: update content and save snapshot")
        void successUpdateContentAndSaveSnapshot() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WikiDocument document = mock(WikiDocument.class);
            UpdateDocumentContentCommand cmd =
                    new UpdateDocumentContentCommand("new content", SemanticUpdateType.PATCH, "fixed typo");

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(mock(WorkspaceMember.class));
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(document);

            // when
            sut.updateContent(workspaceKey, wikiId, cmd, actorMemberId);

            // then
            then(document).should().updateContent("new content", SemanticUpdateType.PATCH);
            then(wikiSnapshotRepository).should().save(any(WikiDocumentSnapshot.class));
        }
    }

    @Nested
    @DisplayName("add link")
    class AddLink {

        @Test
        @DisplayName("success: add link to document")
        void successAddLink() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long targetResourceId = 123L;
            Long actorMemberId = 1L;

            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(mock(WorkspaceMember.class));
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(document);
            given(document.getWorkspaceKey()).willReturn(workspaceKey);
            given(wikiLinkTargetResolver.resolveWorkspaceKey(WikiLinkTargetType.ISSUE, targetResourceId))
                    .willReturn(workspaceKey);

            // when
            sut.addLink(workspaceKey, wikiId, WikiLinkTargetType.ISSUE, targetResourceId, actorMemberId);

            // then
            then(wikiLinkRepository).should().save(any(WikiLink.class));
        }
    }

    @Nested
    @DisplayName("remove link")
    class RemoveLink {

        @Test
        @DisplayName("success: remove link from document")
        void successRemoveLink() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long wikiLinkId = 1L;
            Long actorMemberId = 1L;

            WikiLink link = mock(WikiLink.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(mock(WorkspaceMember.class));
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(mock(WikiDocument.class));
            given(wikiLinkRepository.findByWorkspaceKeyAndId(workspaceKey, wikiLinkId))
                    .willReturn(Optional.of(link));

            // when
            sut.removeLink(workspaceKey, wikiId, wikiLinkId, actorMemberId);

            // then
            then(wikiLinkRepository).should().delete(link);
        }

        @Test
        @DisplayName("fail: throws ResourceNotFoundException when link not found")
        void failRemoveLink_If_LinkNotFound() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long wikiLinkId = 999L;
            Long actorMemberId = 1L;

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(mock(WorkspaceMember.class));
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(mock(WikiDocument.class));
            given(wikiLinkRepository.findByWorkspaceKeyAndId(workspaceKey, wikiLinkId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.removeLink(workspaceKey, wikiId, wikiLinkId, actorMemberId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("lock document")
    class LockDocument {

        @Test
        @DisplayName("success: lock document with authorization check")
        void successLockDocument() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(document);

            // when
            sut.lock(workspaceKey, wikiId, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentLockPermission(document, actor);
            then(document).should().lock();
        }
    }

    @Nested
    @DisplayName("unlock document")
    class UnlockDocument {

        @Test
        @DisplayName("success: unlock document with authorization check")
        void successUnlockDocument() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(document);

            // when
            sut.unLock(workspaceKey, wikiId, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentLockPermission(document, actor);
            then(document).should().unLock();
        }
    }

    @Nested
    @DisplayName("delete document")
    class DeleteDocument {

        @Test
        @DisplayName("success: delete document with authorization check")
        void successDeleteDocument() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentFinder.getBy(workspaceKey, wikiId)).willReturn(document);

            // when
            sut.delete(workspaceKey, wikiId, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(document, actor);
            then(document).should().softDelete();
        }
    }

    @Nested
    @DisplayName("restore document")
    class RestoreDocument {

        @Test
        @DisplayName("success: restore soft-deleted document with authorization check")
        void successRestoreDocument() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentFinder.getDeletedBy(workspaceKey, wikiId)).willReturn(document);

            // when
            sut.restore(workspaceKey, wikiId, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(document, actor);
            then(document).should().restoreSoftDeleted();
        }
    }

    @Nested
    @DisplayName("hard delete document")
    class HardDeleteDocument {

        @Test
        @DisplayName("success: permanently delete soft-deleted document")
        void successHardDeleteDocument() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentFinder.getDeletedBy(workspaceKey, wikiId)).willReturn(document);
            given(wikiDocumentQueryRepository.hasChildren(workspaceKey, wikiId)).willReturn(false);

            // when
            sut.hardDelete(workspaceKey, wikiId, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(document, actor);
            then(wikiDocumentCommandRepository).should().delete(document);
        }

        @Test
        @DisplayName("fail: throws BadRequestException when document has children")
        void failHardDelete_If_HasChildren() {
            // given
            String workspaceKey = "WORKSPACE";
            Long wikiId = 1L;
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument document = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentFinder.getDeletedBy(workspaceKey, wikiId)).willReturn(document);
            given(wikiDocumentQueryRepository.hasChildren(workspaceKey, wikiId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.hardDelete(workspaceKey, wikiId, actorMemberId))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("batch hard delete")
    class BatchHardDelete {

        @Test
        @DisplayName("success: batch hard delete checks permission per document")
        void successBatchHardDelete() {
            // given
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WikiDocument doc1 = mock(WikiDocument.class);
            WikiDocument doc2 = mock(WikiDocument.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(wikiDocumentQueryRepository.findAllDeletedByWorkspaceKey(workspaceKey))
                    .willReturn(List.of(doc1, doc2));

            // when
            sut.batchHardDelete(workspaceKey, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(doc1, actor);
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(doc2, actor);
            then(wikiDocumentCommandRepository).should().deleteAllSoftDeletedByWorkspaceKey(workspaceKey);
        }
    }
}
