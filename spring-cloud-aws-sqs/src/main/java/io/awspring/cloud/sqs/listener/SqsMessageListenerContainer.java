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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.messaging.support.listener.AbstractMessageListenerContainer;
import io.awspring.cloud.messaging.support.listener.AsyncMessageListener;
import io.awspring.cloud.messaging.support.listener.AsyncMessageProducer;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageListenerContainer extends AbstractMessageListenerContainer<String> {

	private final static Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainer.class);

	public SqsMessageListenerContainer(SqsContainerOptions options, SqsAsyncClient sqsClient,
			AsyncMessageListener<String> messageListener, TaskExecutor taskExecutor) {
		super(options, taskExecutor, messageListener, createMessageProducers(options, sqsClient));
	}

	private static List<AsyncMessageProducer<String>> createMessageProducers(SqsContainerOptions options,
			SqsAsyncClient sqsClient) {
		return options.getEndpoint().getQueueAttributes().entrySet().stream()
				.map(entry -> createMessageProducer(sqsClient, entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	private static SqsMessageProducer createMessageProducer(SqsAsyncClient sqsClient, String logicalEndpointName,
			QueueAttributes queueAttributes) {
		return new SqsMessageProducer(logicalEndpointName, queueAttributes, sqsClient);
	}

	@Override
	protected void doStart() {
		logger.debug("Starting SqsMessageListenerContainer: " + this);
		super.getMessageProducers().stream().map(SqsMessageProducer.class::cast).forEach(SqsMessageProducer::start);
	}

	@Override
	protected void doStop() {
		logger.debug("Stopping SqsMessageListenerContainer: " + this);
		super.getMessageProducers().stream().map(SqsMessageProducer.class::cast).forEach(SqsMessageProducer::stop);
	}
}
