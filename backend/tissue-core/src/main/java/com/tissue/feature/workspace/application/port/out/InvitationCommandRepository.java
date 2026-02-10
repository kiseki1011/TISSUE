package com.tissue.feature.workspace.application.port.out;

import com.tissue.feature.workspace.domain.Invitation;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface InvitationCommandRepository extends Repository<Invitation, Long> {

    Invitation save(Invitation invitation);

    List<Invitation> saveAll(Iterable<Invitation> invitations);
}
