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

import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.Map;

/**
 * Options to be used by the template.
 *
 * @param <O> the options subclass to be returned by the chained methods.
 */
public interface MessagingTemplateOptions<O extends MessagingTemplateOptions<O>> {

	/**
	 * Set the acknowledgement mode for this template. Default is {@link TemplateAcknowledgementMode#ACKNOWLEDGE}
	 *
	 * @param acknowledgementMode the mode.
	 * @return the options instance.
	 */
	O acknowledgementMode(TemplateAcknowledgementMode acknowledgementMode);

	/**
	 * Set the strategy to use when handling batch send operations with at least one failed message. Default is
	 * {@link SendBatchFailureHandlingStrategy#THROW}
	 *
	 * @param sendBatchFailureHandlingStrategy the strategy.
	 * @return the options instance.
	 */
	O sendBatchFailureHandlingStrategy(SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy);

	/**
	 * Set the default maximum amount of time this template will wait for the maximum number of messages before
	 * returning. Default is 10 seconds.
	 *
	 * @param defaultPollTimeout the timeout.
	 * @return the options instance.
	 */
	O defaultPollTimeout(Duration defaultPollTimeout);

	/**
	 * Set the default maximum number of messages to be retrieved in a single batch. Default is 10.
	 *
	 * @param defaultMaxNumberOfMessages the maximum number of messages.
	 * @return the options instance.
	 */
	O defaultMaxNumberOfMessages(Integer defaultMaxNumberOfMessages);

	/**
	 * The default class to which this template should convert payloads to.
	 *
	 * @param defaultPayloadClass the default payload class.
	 * @return the options instance.
	 */
	O defaultPayloadClass(Class<?> defaultPayloadClass);

	/**
	 * Set a default header to be added to received messages.
	 *
	 * @param name the header name.
	 * @param value the header value.
	 * @return the options instance.
	 */
	O additionalHeaderForReceive(String name, Object value);

	/**
	 * Set default headers to be added to received messages.
	 *
	 * @param defaultAdditionalHeaders the headers.
	 * @return the options instance.
	 */
	O additionalHeadersForReceive(Map<String, Object> defaultAdditionalHeaders);

	/**
	 * Set the {@link ObservationRegistry} to be used with this template.
	 *
	 * @param observationRegistry the observation registry.
	 * @return the options instance.
	 */
	O observationRegistry(ObservationRegistry observationRegistry);
}
