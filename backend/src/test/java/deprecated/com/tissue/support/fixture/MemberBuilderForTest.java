package deprecated.com.tissue.support.fixture;

import org.springframework.test.util.ReflectionTestUtils;

import com.tissue.api.member.domain.Member;

public class MemberBuilderForTest {

	private Long id = 1L;
	private String email = "mock@example.com";
	private String username = "mockuser";
	private String password = "mock1234!";

	public MemberBuilderForTest id(Long id) {
		this.id = id;
		return this;
	}

	public MemberBuilderForTest email(String email) {
		this.email = email;
		return this;
	}

	public MemberBuilderForTest username(String username) {
		this.username = username;
		return this;
	}

	public MemberBuilderForTest password(String password) {
		this.password = password;
		return this;
	}

	public Member build() {
		Member member = Member.create(
			email,
			username,
			password,
			"GilDong",
			null
		);

		// Set ID using reflection
		ReflectionTestUtils.setField(member, "id", id);
		return member;
	}
}
