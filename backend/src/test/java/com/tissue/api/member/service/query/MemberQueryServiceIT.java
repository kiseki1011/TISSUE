package com.tissue.api.member.service.query;

import org.junit.jupiter.api.AfterEach;

import com.tissue.helper.ServiceIntegrationTestHelper;

class MemberQueryServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

}