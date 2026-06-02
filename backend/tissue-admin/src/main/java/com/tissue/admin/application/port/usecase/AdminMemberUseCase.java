package com.tissue.admin.application.port.usecase;

import com.tissue.admin.application.dto.AdminMemberDetail;
import com.tissue.admin.application.dto.AdminMemberSummary;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminMemberUseCase {

    Page<AdminMemberSummary> listMembers(
            @Nullable MemberStatus status, @Nullable SystemRole role, @Nullable String keyword, Pageable pageable);

    AdminMemberDetail getMember(Long memberId);

    void changeSystemRole(Long targetMemberId, SystemRole newRole, Long actorMemberId);

    void forceWithdraw(Long targetMemberId, Long actorMemberId);

    void forceRestore(Long targetMemberId, Long actorMemberId);

    void revokeSessions(Long targetMemberId, Long actorMemberId);
}
