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

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SQS integration.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SqsAsyncClient.class })
@EnableConfigurationProperties({ SqsProperties.class })
@Import(SqsBootstrapConfiguration.class)
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public SqsAsyncClient sqsAsyncClient(SqsProperties properties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<SqsAsyncClientBuilder>> configurer) {
		return awsClientBuilderConfigurer.configure(SqsAsyncClient.builder(), properties, configurer.getIfAvailable())
				.build();
	}

	@ConditionalOnMissingBean
	@Bean
	public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
			ObjectProvider<SqsAsyncClient> sqsAsyncClient, ObjectProvider<ContainerOptions> containerOptions,
			ObjectProvider<AsyncErrorHandler<Object>> asyncErrorHandler,
			ObjectProvider<ErrorHandler<Object>> errorHandler,
			ObjectProvider<AsyncMessageInterceptor<Object>> asyncInterceptors,
			ObjectProvider<MessageInterceptor<Object>> interceptors) {

		SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
		containerOptions.ifAvailable(
				providedOptions -> factory.configure(options -> options.fromBuilder(providedOptions.toBuilder())));
		sqsAsyncClient.ifAvailable(factory::setSqsAsyncClient);
		asyncErrorHandler.ifAvailable(factory::setErrorHandler);
		errorHandler.ifAvailable(factory::setErrorHandler);
		interceptors.forEach(factory::addMessageInterceptor);
		asyncInterceptors.forEach(factory::addMessageInterceptor);
		return factory;
	}

}
