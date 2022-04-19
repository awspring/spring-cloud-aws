/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.autoconfigure.sns;

import static io.awspring.cloud.sns.configuration.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sns.core.TopicArnResolver;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SNS integration.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Manuel Wessner
 * @author Matej Nedic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SnsClient.class, SnsTemplate.class })
@EnableConfigurationProperties({ SnsProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sns.enabled", havingValue = "true", matchIfMissing = true)
public class SnsAutoConfiguration {

	private final SnsProperties properties;

	public SnsAutoConfiguration(SnsProperties properties) {
		this.properties = properties;
	}

	@ConditionalOnMissingBean
	@Bean
	public SnsClient snsClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		return (SnsClient) awsClientBuilderConfigurer.configure(SnsClient.builder(), this.properties).build();
	}

	@ConditionalOnMissingBean
	@Bean
	public SnsTemplate snsTemplate(SnsClient snsClient, Optional<ObjectMapper> objectMapper,
			Optional<TopicArnResolver> topicArnResolver) {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setSerializedPayloadClass(String.class);
		objectMapper.ifPresent(converter::setObjectMapper);
		return topicArnResolver.map(it -> new SnsTemplate(snsClient, it, converter))
				.orElseGet(() -> new SnsTemplate(snsClient, converter));
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebMvcConfigurer.class)
	static class SnsWebConfiguration {

		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(SnsClient snsClient) {
			return new WebMvcConfigurer() {
				@Override
				public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
					resolvers.add(getNotificationHandlerMethodArgumentResolver(snsClient));
				}
			};
		}

	}

}
