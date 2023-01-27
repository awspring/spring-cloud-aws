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
package io.awspring.cloud.sqs.annotation;

import io.awspring.cloud.sqs.config.Endpoint;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.support.resolver.QueueAttributesMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.SqsMessageMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.VisibilityHandlerMethodArgumentResolver;
import java.util.Arrays;
import java.util.Collection;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * {@link BeanPostProcessor} implementation that scans beans for a {@link SqsListener @SqsListener} annotation, extracts
 * information to a {@link SqsEndpoint}, and registers it in the {@link EndpointRegistrar}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsListenerAnnotationBeanPostProcessor extends AbstractListenerAnnotationBeanPostProcessor<SqsListener> {

	private static final String GENERATED_ID_PREFIX = "io.awspring.cloud.sqs.sqsListenerEndpointContainer#";

	@Override
	protected Class<SqsListener> getAnnotationClass() {
		return SqsListener.class;
	}

	protected Endpoint createEndpoint(SqsListener sqsListenerAnnotation) {
		return SqsEndpoint.builder().queueNames(resolveStringArray(sqsListenerAnnotation.value(), "queueNames"))
				.factoryBeanName(resolveAsString(sqsListenerAnnotation.factory(), "factory"))
				.id(getEndpointId(sqsListenerAnnotation.id()))
				.pollTimeoutSeconds(resolveAsInteger(sqsListenerAnnotation.pollTimeoutSeconds(), "pollTimeoutSeconds"))
				.maxMessagesPerPoll(resolveAsInteger(sqsListenerAnnotation.maxMessagesPerPoll(), "maxMessagesPerPoll"))
				.maxInflightMessagesPerQueue(resolveAsInteger(sqsListenerAnnotation.maxInflightMessagesPerQueue(),
						"maxInflightMessagesPerQueue"))
				.messageVisibility(
						resolveAsInteger(sqsListenerAnnotation.messageVisibilitySeconds(), "messageVisibility"))
				.build();
	}

	@Override
	protected String getGeneratedIdPrefix() {
		return GENERATED_ID_PREFIX;
	}

	@Override
	protected Collection<HandlerMethodArgumentResolver> createAdditionalArgumentResolvers() {
		return Arrays.asList(new VisibilityHandlerMethodArgumentResolver(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER),
				new SqsMessageMethodArgumentResolver(), new QueueAttributesMethodArgumentResolver());
	}

}
