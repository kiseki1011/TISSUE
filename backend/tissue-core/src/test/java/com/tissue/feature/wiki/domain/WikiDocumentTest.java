package com.tissue.feature.wiki.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import com.tissue.feature.wiki.domain.policy.WikiTagConstraintPolicy;
import com.tissue.feature.wiki.domain.vo.SnapshotVersion;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WikiDocumentTest {

    @Nested
    @DisplayName("create document")
    class Create {

        @Test
        @DisplayName("success: create document without parent")
        void successCreateWithoutParent() {
            // when
            WikiDocument document = WikiDocument.create("title", "content", null);

            // then
            assertThat(document.getTitle()).isEqualTo("title");
            assertThat(document.getContent()).isEqualTo("content");
            assertThat(document.isLocked()).isFalse();
            assertThat(document.getParentDocument()).isNull();
            assertThat(document.getCurrentSnapshotVersion()).isEqualTo(SnapshotVersion.initial());
        }

        @Test
        @DisplayName("success: create document with parent")
        void successCreateWithParent() {
            // given
            WikiDocument parent = WikiDocument.create("parent", "parent content", null);

            // when
            WikiDocument child = WikiDocument.create("child", "child content", parent);

            // then
            assertThat(child.getParentDocument()).isEqualTo(parent);
        }
    }

    @Nested
    @DisplayName("update title")
    class UpdateTitle {

        @Test
        @DisplayName("success: update title of unlocked document")
        void successUpdateTitle() {
            // given
            WikiDocument document = WikiDocument.create("old title", "content", null);

            // when
            document.updateTitle("new title");

            // then
            assertThat(document.getTitle()).isEqualTo("new title");
        }

        @Test
        @DisplayName("fail: throws BadRequestException when document is locked")
        void failUpdateTitle_If_Locked() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            document.lock();

            // when & then
            assertThatThrownBy(() -> document.updateTitle("new title")).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("update content")
    class UpdateContent {

        @Test
        @DisplayName("success: update content and bump version")
        void successUpdateContent() {
            // given
            WikiDocument document = WikiDocument.create("title", "old content", null);

            // when
            document.updateContent("new content", SemanticUpdateType.MINOR);

            // then
            assertThat(document.getContent()).isEqualTo("new content");
            assertThat(document.getCurrentSnapshotVersion()).isEqualTo(new SnapshotVersion(1, 1, 0));
        }

        @Test
        @DisplayName("fail: throws BadRequestException when document is locked")
        void failUpdateContent_If_Locked() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            document.lock();

            // when & then
            assertThatThrownBy(() -> document.updateContent("new", SemanticUpdateType.PATCH))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("lock and unlock")
    class LockUnlock {

        @Test
        @DisplayName("success: lock document")
        void successLock() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);

            // when
            document.lock();

            // then
            assertThat(document.isLocked()).isTrue();
        }

        @Test
        @DisplayName("success: unlock document")
        void successUnlock() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            document.lock();

            // when
            document.unLock();

            // then
            assertThat(document.isLocked()).isFalse();
        }

        @Test
        @DisplayName("success: locked document blocks title and content updates")
        void successLockedDocumentBlocksEdits() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            document.lock();

            // when & then
            assertThatThrownBy(() -> document.updateTitle("new")).isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> document.updateContent("new", SemanticUpdateType.PATCH))
                    .isInstanceOf(BadRequestException.class);

            assertThatCode(() -> document.setParent(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: set parent is allowed even when document is locked")
        void successSetParent_When_DocumentLocked() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            document.lock();

            // when & then
            assertThatCode(() -> document.setParent(null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("tags")
    class Tags {

        private WikiTag tag(String name) {
            return WikiTag.create(Name.of(name), null, ColorType.BLUE);
        }

        @Test
        @DisplayName("success: add tag")
        void successAddTag() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            WikiTag tag = tag("architecture");

            // when
            document.addTag(tag);

            // then
            assertThat(document.getTags()).hasSize(1);
            assertThat(document.getTags().iterator().next().getTag()).isEqualTo(tag);
        }

        @Test
        @DisplayName("success: adding the same tag twice is idempotent")
        void successAddTag_Dedup() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            WikiTag tag = tag("architecture");

            // when
            document.addTag(tag);
            document.addTag(tag);

            // then
            assertThat(document.getTags()).hasSize(1);
        }

        @Test
        @DisplayName("success: remove tag")
        void successRemoveTag() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            WikiTag tag = tag("architecture");
            document.addTag(tag);

            // when
            document.removeTag(tag);

            // then
            assertThat(document.getTags()).isEmpty();
        }

        @Test
        @DisplayName("fail: throws when exceeding max tags per document")
        void failAddTag_When_LimitExceeded() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            for (int i = 0; i < WikiTagConstraintPolicy.MAX_TAGS_PER_DOCUMENT; i++) {
                document.addTag(tag("tag-" + i));
            }

            // when & then
            assertThatThrownBy(() -> document.addTag(tag("one-too-many")))
                    .isInstanceOf(ResourceConflictException.class);
            assertThat(document.getTags()).hasSize(WikiTagConstraintPolicy.MAX_TAGS_PER_DOCUMENT);
        }

        @Test
        @DisplayName("success: re-adding an already-attached tag at the limit is a no-op")
        void successAddTag_AtLimit_AlreadyTagged() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            List<WikiTag> tags = new ArrayList<>();
            for (int i = 0; i < WikiTagConstraintPolicy.MAX_TAGS_PER_DOCUMENT; i++) {
                WikiTag t = tag("tag-" + i);
                tags.add(t);
                document.addTag(t);
            }

            // when & then
            assertThatCode(() -> document.addTag(tags.getFirst())).doesNotThrowAnyException();
            assertThat(document.getTags()).hasSize(WikiTagConstraintPolicy.MAX_TAGS_PER_DOCUMENT);
        }

        @Test
        @DisplayName("fail: locked document rejects tagging")
        void failAddTag_When_Locked() {
            // given
            WikiDocument document = WikiDocument.create("title", "content", null);
            document.lock();

            // when & then
            assertThatThrownBy(() -> document.addTag(tag("architecture"))).isInstanceOf(BadRequestException.class);
        }
    }
}
