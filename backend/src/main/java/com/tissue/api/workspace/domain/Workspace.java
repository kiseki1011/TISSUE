package com.tissue.api.workspace.domain;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.workspace.domain.enums.WorkspaceRole.*;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.workspace.domain.exception.WorkspaceOwnershipRequiredException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("softDeleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "workspace_id")
	private Long id;

	@Column(name = "workspace_key", unique = true, nullable = false)
	private String key;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String description;

	public static Workspace create(
		@NonNull String key,
		@NonNull String name,
		@Nullable String description
	) {
		Workspace workspace = new Workspace();
		workspace.key = key;
		workspace.name = name;
		workspace.description = nullToEmpty(description);

		return workspace;
	}

	// TODO: 도메인 서비스로 추출하거나, 애플리케이션 서비스에서 로직 진행하는게 좋을까?
	public void transferOwnership(@NonNull WorkspaceMember owner, @NonNull WorkspaceMember newOwner) {
		if (!owner.isOwner()) {
			// TODO: 상황에 맞는 더 구체적인 예외 이름을 사용하는게 좋을까?
			//  예시: OwnerRequiredForOwnershipTransfer
			throw new WorkspaceOwnershipRequiredException("Needs to be OWNER to transfer ownership.",
				key, owner.getMemberId(), owner.getRole());
		}
		owner.changeRoleTo(ADMIN);
		newOwner.changeRoleToOwner();
	}

	public void updateName(@NonNull String name) {
		this.name = name;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}
}
