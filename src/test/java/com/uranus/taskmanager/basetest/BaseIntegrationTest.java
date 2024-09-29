package com.uranus.taskmanager.basetest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.uranus.taskmanager.api.repository.MemberRepository;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.service.AuthService;
import com.uranus.taskmanager.api.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.service.MemberService;
import com.uranus.taskmanager.fixture.RestAssuredAuthenticationFixture;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
	@LocalServerPort
	protected int port;
	@Autowired
	protected CheckCodeDuplicationService workspaceCreateService;
	@Autowired
	protected AuthService authService;
	@Autowired
	protected MemberService memberService;
	@Autowired
	protected WorkspaceRepository workspaceRepository;
	@Autowired
	protected MemberRepository memberRepository;
	@Autowired
	protected RestAssuredAuthenticationFixture restAssuredAuthenticationFixture;
}
