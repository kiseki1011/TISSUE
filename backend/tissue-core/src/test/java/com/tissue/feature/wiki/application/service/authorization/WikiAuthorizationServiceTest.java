package com.tissue.feature.wiki.application.service.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.wiki.domain.WikiDocument;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WikiAuthorizationServiceTest {

    private final WikiAuthorizationService sut = new WikiAuthorizationService();

    @Nested
    @DisplayName("require document lock permission")
    class RequireDocumentLockPermission {

        @Test
        @DisplayName("success: system admin passes")
        void systemAdminPasses() {
            WikiDocument document = mock(WikiDocument.class);
            Member actor = mock(Member.class);
            given(actor.hasAtLeast(SystemRole.ADMIN)).willReturn(true);

            assertThatCode(() -> sut.requireDocumentLockPermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: document creator passes")
        void creatorPasses() {
            WikiDocument document = mock(WikiDocument.class);
            Member actor = mock(Member.class);
            given(actor.hasAtLeast(SystemRole.ADMIN)).willReturn(false);
            given(actor.getId()).willReturn(1L);
            given(document.getCreatedBy()).willReturn(1L);

            assertThatCode(() -> sut.requireDocumentLockPermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: a non-creator non-admin is rejected")
        void nonCreatorRejected() {
            WikiDocument document = mock(WikiDocument.class);
            Member actor = mock(Member.class);
            given(actor.hasAtLeast(SystemRole.ADMIN)).willReturn(false);
            given(actor.getId()).willReturn(2L);
            given(document.getCreatedBy()).willReturn(1L);

            assertThatThrownBy(() -> sut.requireDocumentLockPermission(document, actor))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("require document delete permission")
    class RequireDocumentDeletePermission {

        @Test
        @DisplayName("success: system admin passes")
        void systemAdminPasses() {
            WikiDocument document = mock(WikiDocument.class);
            Member actor = mock(Member.class);
            given(actor.hasAtLeast(SystemRole.ADMIN)).willReturn(true);

            assertThatCode(() -> sut.requireDocumentDeletePermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("success: document creator passes")
        void creatorPasses() {
            WikiDocument document = mock(WikiDocument.class);
            Member actor = mock(Member.class);
            given(actor.hasAtLeast(SystemRole.ADMIN)).willReturn(false);
            given(actor.getId()).willReturn(1L);
            given(document.getCreatedBy()).willReturn(1L);

            assertThatCode(() -> sut.requireDocumentDeletePermission(document, actor))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("fail: a non-creator non-admin is rejected")
        void nonCreatorRejected() {
            WikiDocument document = mock(WikiDocument.class);
            Member actor = mock(Member.class);
            given(actor.hasAtLeast(SystemRole.ADMIN)).willReturn(false);
            given(actor.getId()).willReturn(2L);
            given(document.getCreatedBy()).willReturn(1L);

            assertThatThrownBy(() -> sut.requireDocumentDeletePermission(document, actor))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
