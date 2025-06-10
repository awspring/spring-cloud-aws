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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.support.observation.SqsListenerObservation;
import java.time.Duration;
import java.util.Collection;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * {@link ContainerOptionsBuilder} specialization for SQS specific options.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface SqsContainerOptionsBuilder
		extends ContainerOptionsBuilder<SqsContainerOptionsBuilder, SqsContainerOptions> {

	/**
	 * Set the {@link QueueAttributeName}s that will be retrieved from the queue and added as headers to the messages.
	 * Default is none.
	 * @param queueAttributeNames the names.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder queueAttributeNames(Collection<QueueAttributeName> queueAttributeNames);

	/**
	 * Set the messageAttributeNames that will be retrieved and added as headers in messages. Default is ALL.
	 * @param messageAttributeNames the names.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder messageAttributeNames(Collection<String> messageAttributeNames);

	/**
	 * Set the {@link MessageSystemAttributeName}s that will be retrieved and added as headers in messages.
	 * @param messageSystemAttributeNames the names.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder messageSystemAttributeNames(
			Collection<MessageSystemAttributeName> messageSystemAttributeNames);

	/**
	 * Set the message visibility for messages retrieved by the container.
	 * @param messageVisibility the visibility timeout.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder messageVisibility(Duration messageVisibility);

	/**
	 * Set how the messages from FIFO queues should be grouped when container listener mode is
	 * {@link ListenerMode#BATCH}. By default, messages are grouped in batches by message group, which are processed in
	 * parallel, maintaining order within each message group.
	 * @param fifoBatchGroupingStrategy the strategy to batch FIFO messages.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder fifoBatchGroupingStrategy(FifoBatchGroupingStrategy fifoBatchGroupingStrategy);

	/**
	 * Set the {@link QueueNotFoundStrategy} for the container.
	 * @param queueNotFoundStrategy the strategy.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder queueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy);

	/**
	 * Set a custom SqsListenerObservation.Convention to be used in this container.
	 * @param observationConvention the custom observation convention.
	 * @return this instance.
	 */
	SqsContainerOptionsBuilder observationConvention(SqsListenerObservation.Convention observationConvention);

}
