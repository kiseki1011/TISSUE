package deprecated.com.tissue.support.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import deprecated.com.tissue.support.mock.MockAuthorizationInterceptor;
import deprecated.com.tissue.support.mock.MockCurrentMemberArgumentResolver;

@TestConfiguration
public class WebMvcTestConfig implements WebMvcConfigurer {

	@Value("${test.allow.hasSufficientRole:true}")
	private boolean hasSufficientRole;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MockAuthorizationInterceptor(hasSufficientRole))
			.order(1);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new MockCurrentMemberArgumentResolver());
	}
}
