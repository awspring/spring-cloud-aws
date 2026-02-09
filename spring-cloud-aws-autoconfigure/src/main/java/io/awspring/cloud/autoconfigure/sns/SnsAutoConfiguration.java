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
import static io.awspring.cloud.sns.configuration.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolverLegacyJackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.core.support.JacksonPresent;
import io.awspring.cloud.sns.core.CachingTopicArnResolver;
import io.awspring.cloud.sns.core.DefaultTopicArnResolver;
import io.awspring.cloud.sns.core.SnsOperations;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sns.core.TopicArnResolver;
import io.awspring.cloud.sns.core.batch.SnsBatchOperations;
import io.awspring.cloud.sns.core.batch.SnsBatchTemplate;
import io.awspring.cloud.sns.core.batch.converter.DefaultSnsMessageConverter;
import io.awspring.cloud.sns.core.batch.converter.SnsMessageConverter;
import io.awspring.cloud.sns.core.batch.executor.BatchExecutionStrategy;
import io.awspring.cloud.sns.core.batch.executor.SequentialBatchExecutionStrategy;
import io.awspring.cloud.sns.sms.SnsSmsOperations;
import io.awspring.cloud.sns.sms.SnsSmsTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.services.sns.SnsClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SNS integration.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Manuel Wessner
 * @author Matej Nedic
 * @author Mariusz Sondecki
 */
@AutoConfiguration
@ConditionalOnClass({ SnsClient.class, SnsTemplate.class })
@EnableConfigurationProperties({ SnsProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sns.enabled", havingValue = "true", matchIfMissing = true)
public class SnsAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public SnsClient snsClient(SnsProperties properties, AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails,
			ObjectProvider<SnsClientCustomizer> snsClientCustomizers,
			ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers) {
		return awsClientBuilderConfigurer
				.configureSyncClient(SnsClient.builder(), properties, connectionDetails.getIfAvailable(),
						snsClientCustomizers.orderedStream(), awsSyncClientCustomizers.orderedStream())
				.build();
	}

	@ConditionalOnMissingBean(SnsSmsOperations.class)
	@Bean
	public SnsSmsTemplate snsSmsTemplate(SnsClient snsClient) {
		return new SnsSmsTemplate(snsClient);
	}

	@ConditionalOnClass(name = "tools.jackson.databind.json.JsonMapper")
	@Configuration
	static class SnsConfiguration {
		@ConditionalOnMissingBean(SnsOperations.class)
		@Bean
		public SnsTemplate snsTemplate(SnsClient snsClient, Optional<JsonMapper> jsonMapper,
				Optional<TopicArnResolver> topicArnResolver, ObjectProvider<ChannelInterceptor> interceptors) {
			JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(
					jsonMapper.orElseGet(JsonMapper::new));
			converter.setSerializedPayloadClass(String.class);
			SnsTemplate snsTemplate = topicArnResolver.map(it -> new SnsTemplate(snsClient, it, converter))
					.orElseGet(() -> new SnsTemplate(snsClient, converter));
			interceptors.forEach(snsTemplate::addChannelInterceptor);

			return snsTemplate;
		}
	}

	@ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
	@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
	@Configuration
	static class LegacyJackson2Configuration {
		@ConditionalOnMissingBean(SnsOperations.class)
		@Bean
		public SnsTemplate snsTemplate(SnsClient snsClient, Optional<ObjectMapper> objectMapper,
				Optional<TopicArnResolver> topicArnResolver, ObjectProvider<ChannelInterceptor> interceptors) {
			MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
			converter.setSerializedPayloadClass(String.class);
			objectMapper.ifPresent(converter::setObjectMapper);
			SnsTemplate snsTemplate = topicArnResolver.map(it -> new SnsTemplate(snsClient, it, converter))
					.orElseGet(() -> new SnsTemplate(snsClient, converter));
			interceptors.forEach(snsTemplate::addChannelInterceptor);

			return snsTemplate;
		}
	}

	@ConditionalOnClass(name = "tools.jackson.databind.json.JsonMapper")
	@Configuration
	static class SnsBatchConfiguration {
		@ConditionalOnMissingBean(SnsMessageConverter.class)
		@Bean
		public DefaultSnsMessageConverter defaultSnsMessageConverter(Optional<JsonMapper> jsonMapper) {
			JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(
					jsonMapper.orElseGet(JsonMapper::new));
			converter.setSerializedPayloadClass(String.class);
			return new DefaultSnsMessageConverter(converter);
		}

		@ConditionalOnMissingBean(BatchExecutionStrategy.class)
		@Bean
		public SequentialBatchExecutionStrategy defaultBatchExecutionStrategy(SnsClient snsClient) {
			return new SequentialBatchExecutionStrategy(snsClient);
		}

		@ConditionalOnMissingBean(SnsBatchOperations.class)
		@Bean
		public SnsBatchTemplate snsBatchTemplate(SnsMessageConverter snsMessageConverter,
				BatchExecutionStrategy batchExecutionStrategy, SnsClient snsClient,
				Optional<TopicArnResolver> topicArnResolver) {
			return new SnsBatchTemplate(snsMessageConverter, batchExecutionStrategy, topicArnResolver
					.orElseGet(() -> new CachingTopicArnResolver(new DefaultTopicArnResolver(snsClient))));
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebMvcConfigurer.class)
	static class SnsWebConfiguration {

		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(SnsClient snsClient) {
			if (JacksonPresent.isJackson3Present()) {
				return new WebMvcConfigurer() {
					@Override
					public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
						resolvers.add(getNotificationHandlerMethodArgumentResolver(snsClient));
					}
				};
			}
			else if (JacksonPresent.isJackson2Present()) {
				return new WebMvcConfigurer() {
					@Override
					public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
						resolvers.add(getNotificationHandlerMethodArgumentResolverLegacyJackson2(snsClient));
					}
				};
			}
			throw new IllegalStateException(
					"SecretsManagerPropertySource requires a Jackson 2 or Jackson 3 library on the classpath");
		}
	}

}
