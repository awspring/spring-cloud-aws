/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import java.util.function.Consumer;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Builder interface for creating a {@link SqsTemplate} instance.
 *
 */
public interface SqsTemplateBuilder {

	/**
	 * Set the {@link SqsAsyncClient} to be used by the {@link SqsTemplate}.
	 *
	 * @param sqsAsyncClient the instance.
	 * @return the builder.
	 */
	SqsTemplateBuilder sqsAsyncClient(SqsAsyncClient sqsAsyncClient);

	/**
	 * Set the {@link MessagingMessageConverter} to be used by the template.
	 *
	 * @param messageConverter the converter.
	 * @return the builder.
	 */
	SqsTemplateBuilder messageConverter(MessagingMessageConverter<Message> messageConverter);

	/**
	 * Configure the default message converter.
	 *
	 * @param messageConverterConfigurer a {@link SqsMessagingMessageConverter} consumer.
	 * @return the builder.
	 */
	SqsTemplateBuilder configureDefaultConverter(Consumer<SqsMessagingMessageConverter> messageConverterConfigurer);

	/**
	 * Configure options for the template.
	 *
	 * @param options a {@link SqsTemplateOptions} consumer.
	 * @return the builder.
	 */
	SqsTemplateBuilder configure(Consumer<SqsTemplateOptions> options);

	/**
	 * Create the template with the provided options, exposing both sync and async methods.
	 *
	 * @return the {@link SqsTemplate} instance.
	 */
	SqsTemplate build();

	/**
	 * Create the template with the provided options, exposing only the async methods contained in the
	 * {@link SqsAsyncOperations} interface.
	 *
	 * @return the {@link SqsTemplate} instance.
	 */
	SqsAsyncOperations buildAsyncTemplate();

	/**
	 * Create the template with the provided options, exposing only the sync methods contained in the
	 * {@link SqsOperations} interface.
	 *
	 * @return the {@link SqsTemplate} instance.
	 */
	SqsOperations buildSyncTemplate();

}
