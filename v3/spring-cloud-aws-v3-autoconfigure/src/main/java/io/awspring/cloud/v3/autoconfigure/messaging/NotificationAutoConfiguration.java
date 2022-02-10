package io.awspring.cloud.v3.autoconfigure.messaging;

import static io.awspring.cloud.v3.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;

import java.util.List;

import io.awspring.cloud.v3.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.sns.SnsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;


@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SnsClient.class, SnsAsyncClient.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class,
	RegionProviderAutoConfiguration.class,
	SnsAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sns.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationAutoConfiguration {

	@Configuration
	@ConditionalOnClass(WebMvcConfigurer.class)
	static class SnsWebConfiguration {

		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(final SnsClient amazonSns) {
			return new WebMvcConfigurer() {
				@Override
				public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
					resolvers.add(getNotificationHandlerMethodArgumentResolver(amazonSns));
				}
			};
		}

	}
}
