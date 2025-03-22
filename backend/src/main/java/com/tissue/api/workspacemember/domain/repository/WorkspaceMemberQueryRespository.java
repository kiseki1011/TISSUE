package com.tissue.api.workspacemember.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public interface WorkspaceMemberQueryRespository extends JpaRepository<WorkspaceMember, Long> {
}
