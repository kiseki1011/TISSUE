package com.tissue.feature.wiki.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.wiki.domain.enums.WikiLinkTargetType;
import com.tissue.shared.exception.base.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WikiLinkTest {

    @Nested
    @DisplayName("create link")
    class Create {

        @Test
        @DisplayName("success: create link to different wiki document")
        void successCreateLinkToDifferentDocument() {
            // given
            WikiDocument source = mockDocument(1L);

            // when
            WikiLink link = WikiLink.create(source, WikiLinkTargetType.WIKI_DOC, 2L);

            // then
            assertThat(link.getSourceDocument()).isEqualTo(source);
            assertThat(link.getTargetType()).isEqualTo(WikiLinkTargetType.WIKI_DOC);
            assertThat(link.getTargetId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("success: create link to issue (self-reference check does not apply)")
        void successCreateLinkToIssue() {
            // given
            WikiDocument source = mockDocument(1L);

            // when & then
            assertThatCode(() -> WikiLink.create(source, WikiLinkTargetType.ISSUE, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: throws BadRequestException when linking wiki document to itself")
        void failCreate_If_SelfReference() {
            // given
            WikiDocument source = mockDocument(1L);

            // when & then
            assertThatThrownBy(() -> WikiLink.create(source, WikiLinkTargetType.WIKI_DOC, 1L))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    private WikiDocument mockDocument(Long id) {
        WikiDocument document = mock(WikiDocument.class);
        given(document.getId()).willReturn(id);
        return document;
    }
}
