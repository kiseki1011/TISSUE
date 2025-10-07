package deprecated.com.tissue.unit.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.global.key.WorkspaceKeyGenerator;

class WorkspaceKeyGeneratorTest {

	private WorkspaceKeyGenerator workspaceKeyGenerator;

	@BeforeEach
	void setUp() {
		workspaceKeyGenerator = new WorkspaceKeyGenerator();
	}

	@Test
	@DisplayName("워크스페이스 코드 생성기에 의해 8자리 코드가 생성되어야 한다")
	void testGenerateWorkspaceCodeLength() {

		String workspaceCode = workspaceKeyGenerator.generateWorkspaceKeySuffix();

		assertThat(workspaceCode).isNotNull();
		assertThat(workspaceCode.length()).isEqualTo(8);
	}

	@Test
	@DisplayName("워크스페이스 코드 생성기에 의해 생성된 코드는 Base62 형식이어야 한다")
	void testGenerateWorkspaceCodeFormat() {

		String workspaceCode = workspaceKeyGenerator.generateWorkspaceKeySuffix();

		assertThat(workspaceCode).matches("[A-Za-z0-9]+");
	}

}
