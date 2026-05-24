package com.tissue.feature.workspace.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.dto.response.query.InvitationDetail;
import com.tissue.feature.workspace.application.port.repository.InvitationQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.InvitationQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvitationQueryService implements InvitationQueryUseCase {

    private final InvitationQueryRepository invitationQueryRepository;
    private final MemberFinder memberFinder;

    @Override
    public List<InvitationDetail> getMyInvitations(Long memberId) {
        // TODO: N+1, consider optimization
        return invitationQueryRepository.findAllByMemberId(memberId).stream()
                .map(invitation -> {
                    Member inviter = memberFinder
                            .getOptionalActiveById(invitation.getCreatedBy())
                            .orElse(null);
                    return InvitationDetail.from(invitation, inviter);
                })
                .toList();
    }
}
