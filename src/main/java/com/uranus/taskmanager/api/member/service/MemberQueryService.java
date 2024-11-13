package com.uranus.taskmanager.api.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.dto.request.UpdateAuthRequest;
import com.uranus.taskmanager.api.member.exception.InvalidMemberPasswordException;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MemberQueryService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public void validatePasswordForUpdate(UpdateAuthRequest request, Long id) {
		Member member = memberRepository.findById(id)
			.orElseThrow(MemberNotFoundException::new);

		if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
			throw new InvalidMemberPasswordException();
		}
	}
}
