package com.tissue.feature.wiki.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
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
    private MemberFinder memberFinder;

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
            Long actorMemberId = 1L;
            DocumentCreateCommand cmd = new DocumentCreateCommand("title", "content", null);

            // when
            DocumentResponse response = sut.create(cmd, actorMemberId);

            // then
            then(wikiDocumentCommandRepository).should().save(any(WikiDocument.class));
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("success: create document with parent")
        void successCreateDocumentWithParent() {
            // given
            Long actorMemberId = 1L;
            Long parentDocumentId = 123L;
            WikiDocument parentDocument = mock(WikiDocument.class);
            DocumentCreateCommand cmd = new DocumentCreateCommand("title", "content", parentDocumentId);

            given(wikiDocumentFinder.getById(parentDocumentId)).willReturn(parentDocument);

            // when
            DocumentResponse response = sut.create(cmd, actorMemberId);

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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            WikiDocument document = mock(WikiDocument.class);

            given(wikiDocumentFinder.getById(wikiId)).willReturn(document);

            // when
            sut.updateTitle(wikiId, "new title", actorMemberId);

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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            WikiDocument document = mock(WikiDocument.class);
            UpdateDocumentContentCommand cmd =
                    new UpdateDocumentContentCommand("new content", SemanticUpdateType.PATCH, "fixed typo");

            given(wikiDocumentFinder.getById(wikiId)).willReturn(document);

            // when
            sut.updateContent(wikiId, cmd, actorMemberId);

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
            Long wikiId = 1L;
            Long targetResourceId = 123L;
            Long actorMemberId = 1L;
            WikiDocument document = mock(WikiDocument.class);

            given(wikiDocumentFinder.getById(wikiId)).willReturn(document);

            // when
            sut.addLink(wikiId, WikiLinkTargetType.ISSUE, targetResourceId, actorMemberId);

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
            Long wikiId = 1L;
            Long wikiLinkId = 1L;
            Long actorMemberId = 1L;
            WikiLink link = mock(WikiLink.class);

            given(wikiDocumentFinder.getById(wikiId)).willReturn(mock(WikiDocument.class));
            given(wikiLinkRepository.findById(wikiLinkId)).willReturn(Optional.of(link));

            // when
            sut.removeLink(wikiId, wikiLinkId, actorMemberId);

            // then
            then(wikiLinkRepository).should().delete(link);
        }

        @Test
        @DisplayName("fail: throws ResourceNotFoundException when link not found")
        void failRemoveLink_If_LinkNotFound() {
            // given
            Long wikiId = 1L;
            Long wikiLinkId = 999L;
            Long actorMemberId = 1L;

            given(wikiDocumentFinder.getById(wikiId)).willReturn(mock(WikiDocument.class));
            given(wikiLinkRepository.findById(wikiLinkId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.removeLink(wikiId, wikiLinkId, actorMemberId))
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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument document = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentFinder.getById(wikiId)).willReturn(document);

            // when
            sut.lock(wikiId, actorMemberId);

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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument document = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentFinder.getById(wikiId)).willReturn(document);

            // when
            sut.unLock(wikiId, actorMemberId);

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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument document = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentFinder.getById(wikiId)).willReturn(document);

            // when
            sut.delete(wikiId, actorMemberId);

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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument document = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentFinder.getDeletedById(wikiId)).willReturn(document);

            // when
            sut.restore(wikiId, actorMemberId);

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
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument document = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentFinder.getDeletedById(wikiId)).willReturn(document);
            given(wikiDocumentQueryRepository.hasChildren(wikiId)).willReturn(false);

            // when
            sut.hardDelete(wikiId, actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(document, actor);
            then(wikiDocumentCommandRepository).should().delete(document);
        }

        @Test
        @DisplayName("fail: throws BadRequestException when document has children")
        void failHardDelete_If_HasChildren() {
            // given
            Long wikiId = 1L;
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument document = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentFinder.getDeletedById(wikiId)).willReturn(document);
            given(wikiDocumentQueryRepository.hasChildren(wikiId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.hardDelete(wikiId, actorMemberId)).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("batch hard delete")
    class BatchHardDelete {

        @Test
        @DisplayName("success: batch hard delete checks permission per document")
        void successBatchHardDelete() {
            // given
            Long actorMemberId = 1L;
            Member actor = mock(Member.class);
            WikiDocument doc1 = mock(WikiDocument.class);
            WikiDocument doc2 = mock(WikiDocument.class);

            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(wikiDocumentQueryRepository.findAllDeleted()).willReturn(List.of(doc1, doc2));

            // when
            sut.batchHardDelete(actorMemberId);

            // then
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(doc1, actor);
            then(wikiAuthorizationService).should().requireDocumentDeletePermission(doc2, actor);
            then(wikiDocumentCommandRepository).should().deleteAllSoftDeleted();
        }
    }
}
