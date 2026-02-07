package com.tissue.workspace.application.service.finder;

import com.tissue.member.domain.Member;
import com.tissue.workspace.application.port.out.InvitationQueryRepository;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.exception.InvitationNotFoundException;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationFinder {

    private final InvitationQueryRepository invitationQueryRepository;

    public Invitation getBy(Long id, Member member) {
        return invitationQueryRepository
                .findByIdAndMember(id, member)
                .orElseThrow(() -> new InvitationNotFoundException(id, member.getId()));
    }

    public Set<Long> findPendingMemberIds(String workspaceKey, Collection<Long> memberIds) {
        return invitationQueryRepository.findPendingMemberIds(workspaceKey, memberIds);
    }
}
