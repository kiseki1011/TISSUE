package com.uranus.taskmanager.api.member.dto.response;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.dto.MemberDetail;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberEmailUpdateResponse {
	private MemberDetail memberDetail;

	public static MemberEmailUpdateResponse from(Member member) {
		return new MemberEmailUpdateResponse(MemberDetail.from(member));
	}
}
