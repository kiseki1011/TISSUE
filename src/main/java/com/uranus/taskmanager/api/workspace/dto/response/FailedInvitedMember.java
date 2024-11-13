package com.uranus.taskmanager.api.workspace.dto.response;

import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class FailedInvitedMember {
	private final String identifier;
	private final String error;

	@Builder
	public FailedInvitedMember(String identifier, String error) {
		this.identifier = identifier;
		this.error = error;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FailedInvitedMember that = (FailedInvitedMember)o;
		return Objects.equals(identifier, that.identifier) && Objects.equals(error, that.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, error);
	}
}
