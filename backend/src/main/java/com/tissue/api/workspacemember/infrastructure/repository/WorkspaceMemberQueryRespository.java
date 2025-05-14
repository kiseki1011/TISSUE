package com.tissue.api.workspacemember.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

public interface WorkspaceMemberQueryRespository extends JpaRepository<WorkspaceMember, Long> {
}
