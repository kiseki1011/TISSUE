package com.tissue.feature.wiki.application.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.feature.wiki.domain.vo.SnapshotVersion;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WikiDocumentSearchResultTest {

    @Nested
    @DisplayName("extract snippet")
    class ExtractSnippet {

        @Test
        @DisplayName("success: snippet centers around keyword in content")
        void successSnippetCentersAroundKeyword() {
            // given
            String padding = "x".repeat(100);
            String content = padding + "KEYWORD" + padding;
            WikiDocument document = mockDocument(content);

            // when
            WikiDocumentSearchResult result = WikiDocumentSearchResult.from(document, "KEYWORD");

            // then
            assertThat(result.contentSnippet()).contains("KEYWORD");
            assertThat(result.contentSnippet().length()).isLessThanOrEqualTo(200);
        }

        @Test
        @DisplayName("success: snippet falls back to beginning when keyword not found in content")
        void successSnippetFallsBackToBeginning() {
            // given
            String content = "The beginning of the document content if no search keyword";
            WikiDocument document = mockDocument(content);

            // when
            WikiDocumentSearchResult result = WikiDocumentSearchResult.from(document, "nonexistent");

            // then
            assertThat(result.contentSnippet()).isEqualTo(content);
        }

        @Test
        @DisplayName("success: snippet can handle keyword at the very beginning")
        void successSnippetKeywordAtBeginning() {
            // given
            String content = "keyword" + "x".repeat(300);
            WikiDocument document = mockDocument(content);

            // when
            WikiDocumentSearchResult result = WikiDocumentSearchResult.from(document, "keyword");

            // then
            assertThat(result.contentSnippet()).startsWith("keyword");
            assertThat(result.contentSnippet().length()).isEqualTo(200);
        }

        @Test
        @DisplayName("success: snippet can handle short content")
        void successSnippetShortContent() {
            // given
            String content = "short content";
            WikiDocument document = mockDocument(content);

            // when
            WikiDocumentSearchResult result = WikiDocumentSearchResult.from(document, "short");

            // then
            assertThat(result.contentSnippet()).isEqualTo(content);
        }

        @Test
        @DisplayName("success: keyword matching is not case sensitive")
        void successKeywordMatchingCaseNotSensitive() {
            // given
            String padding = "x".repeat(300);
            String content = padding + "This document contains 'Keyword' here" + padding;
            WikiDocument document = mockDocument(content);

            // when
            WikiDocumentSearchResult result = WikiDocumentSearchResult.from(document, "keyword");

            // then
            assertThat(result.contentSnippet()).contains("Keyword");
        }
    }

    private WikiDocument mockDocument(String content) {
        WikiDocument document = mock(WikiDocument.class);
        given(document.getId()).willReturn(1L);
        given(document.getTitle()).willReturn("title");
        given(document.getContent()).willReturn(content);
        given(document.isLocked()).willReturn(false);
        given(document.getCurrentSnapshotVersion()).willReturn(SnapshotVersion.initial());
        given(document.getCreatedAt()).willReturn(Instant.now());
        given(document.getLastModifiedAt()).willReturn(Instant.now());
        return document;
    }
}
