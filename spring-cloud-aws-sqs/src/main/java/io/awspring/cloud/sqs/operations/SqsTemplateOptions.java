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

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.support.observation.SqsTemplateObservation;
import java.util.Collection;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Sqs specific options for the {@link SqsTemplate}.
 *
 */
public interface SqsTemplateOptions extends MessagingTemplateOptions<SqsTemplateOptions> {

	/**
	 * Set the default queue for this template. Default is blank.
	 *
	 * @param defaultQueue the default queue.
	 * @return the options instance.
	 */
	SqsTemplateOptions defaultQueue(String defaultQueue);

	/**
	 * The {@link QueueNotFoundStrategy} for this template.
	 *
	 * @param queueNotFoundStrategy the strategy.
	 * @return the options instance.
	 */
	SqsTemplateOptions queueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy);

	/**
	 * The queue attribute names that will be retrieved by this template and added as headers to received messages.
	 * Default is none.
	 *
	 * @param queueAttributeNames the names.
	 * @return the options instance.
	 */
	SqsTemplateOptions queueAttributeNames(Collection<QueueAttributeName> queueAttributeNames);

	/**
	 * The message attributes to be retrieved with the message and added as headers to received messages. Default is
	 * ALL.
	 *
	 * @param messageAttributeNames the names.
	 * @return the options instance.
	 */
	SqsTemplateOptions messageAttributeNames(Collection<String> messageAttributeNames);

	/**
	 * The message system attributes to be retrieved with the message and added as headers to received messages. Default
	 * is ALL.
	 *
	 * @param messageSystemAttributeNames the names.
	 * @return the options instance.
	 */
	SqsTemplateOptions messageSystemAttributeNames(Collection<MessageSystemAttributeName> messageSystemAttributeNames);

	/**
	 * Set the ContentBasedDeduplication queue attribute value of the queues the template is sending messages to. By
	 * default, this is set to AUTO and the queue attribute value will be resolved automatically per queue. If set to
	 * ENABLED or DISABLED, the value will apply to all queues.
	 *
	 * @param contentBasedDeduplication the ContentBasedDeduplication value.
	 * @return the options instance.
	 */
	SqsTemplateOptions contentBasedDeduplication(TemplateContentBasedDeduplication contentBasedDeduplication);

	/**
	 * Set a custom {@link io.micrometer.observation.ObservationConvention} to be used by this template.
	 * @param observationConvention the custom observation convention.
	 * @return the options instance.
	 */
	SqsTemplateOptions observationConvention(SqsTemplateObservation.Convention observationConvention);
}
