package com.tissue.feature.member.application.port.repository;

import com.tissue.shared.enums.SupportedLanguage;

/**
 * Projection interface for member contact information used by the notification domain.
 *
 * <p><strong>Warning:</strong> This interface is returned as a Spring Data JPA Proxy.
 * Therefore, Object comparison (equals/hashCode) is not possible.
 *
 * <p>When using this interface in Collections or filtering:
 * <ul>
 *     <li>Always compare explicitly using {@link #getMemberId()}.</li>
 * </ul>
 */
public interface MemberContactInfo {
    Long getMemberId();

    String getEmail();

    SupportedLanguage getLanguage();
}
