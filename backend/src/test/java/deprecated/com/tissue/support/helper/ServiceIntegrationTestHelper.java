package deprecated.com.tissue.support.helper;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"jwt.secret=ThisIsADefaultTestSecretThatIs32Chars"})
@AutoConfigureMockMvc
public abstract class ServiceIntegrationTestHelper {}
