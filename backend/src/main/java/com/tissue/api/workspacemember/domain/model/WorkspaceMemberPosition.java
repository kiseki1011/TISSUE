package com.tissue.api.workspacemember.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.position.domain.model.Position;

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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_workspace_member_position",
			columnNames = {"workspace_member_id", "position_id"})
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = {"workspaceMember", "position"}, callSuper = false)
public class WorkspaceMemberPosition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "position_id", nullable = false)
	private Position position;

	@Builder
	public WorkspaceMemberPosition(WorkspaceMember workspaceMember, Position position) {
		this.workspaceMember = workspaceMember;
		this.position = position;

		workspaceMember.getWorkspaceMemberPositions().add(this);
		position.getWorkspaceMemberPositions().add(this);
	}
}
