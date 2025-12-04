package com.tissue.api.workspace.application.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.api.workspace.domain.Invitation;

public interface InvitationCommandRepository extends Repository<Invitation, Long> {

	Invitation save(Invitation invitation);

	List<Invitation> saveAll(Iterable<Invitation> invitations);
}
