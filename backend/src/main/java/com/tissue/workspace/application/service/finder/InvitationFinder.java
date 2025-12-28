package com.tissue.workspace.application.service.finder;

import com.tissue.member.domain.Member;
import com.tissue.workspace.application.port.out.InvitationQueryRepository;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
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
                .orElseThrow(() -> WorkspaceExceptions.invitationNotFound(id, member.getId()));
    }

    public Invitation getBy(Long id) {
        return invitationQueryRepository
                .findById(id)
                .orElseThrow(() -> WorkspaceExceptions.invitationNotFound(id));
    }

    public Set<Long> findPendingMemberIds(String workspaceKey, Collection<Long> memberIds) {
        return invitationQueryRepository.findPendingMemberIds(workspaceKey, memberIds);
    }
}
