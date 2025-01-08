package com.tissue.api.position.domain;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "UK_WORKSPACE_MEMBER_POSITION",
			columnNames = {"WORKSPACE_MEMBER_ID", "POSITION_ID"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberPosition extends WorkspaceContextBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_MEMBER_ID", nullable = false)
	private WorkspaceMember workspaceMember;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "POSITION_ID", nullable = false)
	private Position position;

	@Builder
	public WorkspaceMemberPosition(WorkspaceMember workspaceMember, Position position) {
		this.workspaceMember = workspaceMember;
		this.position = position;

		workspaceMember.getWorkspaceMemberPositions().add(this);
		position.getWorkspaceMemberPositions().add(this);
	}
}
