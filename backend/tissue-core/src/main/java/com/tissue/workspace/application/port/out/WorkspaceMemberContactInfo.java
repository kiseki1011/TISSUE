package com.tissue.workspace.application.port.out;

import com.tissue.common.enums.SupportedLanguage;

/**
 * Projection interface for WorkspaceMember with Contact info.
 *
 * <p><strong>Warning:</strong> This interface is returned as a Spring Data JPA Proxy.
 * Therefore, Object comparison (equals/hashCode) is not possible.
 *
 * <p>When using this interface in Collections or filtering:
 * <ul>
 *     <li>Always compare explicitly using {@link #getMemberId()}.</li>
 * </ul>
 */
public interface WorkspaceMemberContactInfo {
    Long getMemberId();

    String getEmail();

    SupportedLanguage getLanguage();
}
