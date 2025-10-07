package deprecated.com.tissue.integration.service.query;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class MemberQueryServiceIT extends ServiceIntegrationTestHelper {

	// @BeforeEach
	// public void setUp() {
	// 	// create member, loginId: "tester", email: "tester@test.com"
	// 	testDataFixture.createMember("tester");
	// }
	//
	// @AfterEach
	// public void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("나의 상세 정보를 조회할 수 있다")
	// void test() {
	// 	// given
	// 	Member member = memberRepository.findByLoginId("tester").get();
	// 	Long memberId = member.getId();
	//
	// 	// when
	// 	GetProfileResponse response = memberQueryService.getProfile(memberId);
	//
	// 	// then
	// 	assertThat(response.loginId()).isEqualTo("tester");
	// 	assertThat(response.email()).isEqualTo("tester@test.com");
	// }
}