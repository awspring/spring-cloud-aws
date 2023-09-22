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
package io.awspring.cloud.autoconfigure.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.operations.SqsTemplateBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SQS integration.
 *
 * @author Tomaz Fernandes
 * @author Maciej Walkowiak
 * @since 3.0
 */
@AutoConfiguration
@ConditionalOnClass({ SqsAsyncClient.class, SqsBootstrapConfiguration.class })
@EnableConfigurationProperties(SqsProperties.class)
@Import(SqsBootstrapConfiguration.class)
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsAutoConfiguration {

	private final SqsProperties sqsProperties;

	public SqsAutoConfiguration(SqsProperties sqsProperties) {
		this.sqsProperties = sqsProperties;
	}

	@ConditionalOnMissingBean
	@Bean
	public SqsAsyncClient sqsAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<SqsAsyncClientBuilder>> configurer) {
		return awsClientBuilderConfigurer
				.configure(SqsAsyncClient.builder(), this.sqsProperties, configurer.getIfAvailable()).build();
	}

	@ConditionalOnMissingBean
	@Bean
	public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient, ObjectProvider<ObjectMapper> objectMapperProvider) {
		SqsTemplateBuilder builder = SqsTemplate.builder().sqsAsyncClient(sqsAsyncClient);
		objectMapperProvider
				.ifAvailable(om -> builder.configureDefaultConverter(converter -> converter.setObjectMapper(om)));
		return builder.build();
	}

	@ConditionalOnMissingBean
	@Bean
	public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
			ObjectProvider<SqsAsyncClient> sqsAsyncClient, ObjectProvider<AsyncErrorHandler<Object>> asyncErrorHandler,
			ObjectProvider<ErrorHandler<Object>> errorHandler,
			ObjectProvider<AsyncMessageInterceptor<Object>> asyncInterceptors,
			ObjectProvider<MessageInterceptor<Object>> interceptors) {

		SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
		factory.configure(this::configureContainerOptions);
		sqsAsyncClient.ifAvailable(factory::setSqsAsyncClient);
		asyncErrorHandler.ifAvailable(factory::setErrorHandler);
		errorHandler.ifAvailable(factory::setErrorHandler);
		interceptors.forEach(factory::addMessageInterceptor);
		asyncInterceptors.forEach(factory::addMessageInterceptor);
		return factory;
	}

	private void configureContainerOptions(ContainerOptionsBuilder<?, ?> options) {
		PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		mapper.from(this.sqsProperties.getListener().getMaxConcurrentMessages()).to(options::maxConcurrentMessages);
		mapper.from(this.sqsProperties.getListener().getMaxMessagesPerPoll()).to(options::maxMessagesPerPoll);
		mapper.from(this.sqsProperties.getListener().getPollTimeout()).to(options::pollTimeout);
	}

	@Bean
	public SqsListenerConfigurer objectMapperCustomizer(ObjectProvider<ObjectMapper> objectMapperProvider) {
		ObjectMapper objectMapper = objectMapperProvider.getIfUnique();
		return registrar -> {
			if (registrar.getObjectMapper() == null && objectMapper != null) {
				registrar.setObjectMapper(objectMapper);
			}
		};
	}

}
