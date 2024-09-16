package com.uranus.taskmanager.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.member.Member;
import com.uranus.taskmanager.api.repository.MemberRepository;
import com.uranus.taskmanager.api.request.SignupRequest;
import com.uranus.taskmanager.api.response.SignupResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final MemberRepository memberRepository;

	@Transactional
	public SignupResponse signup(SignupRequest signupRequest) {
		Member member = signupRequest.toEntity();
		// Todo: password 암호화
		return SignupResponse.fromEntity(memberRepository.save(member));
	}

}
