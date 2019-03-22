/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.core.CachingDestinationResolverProxy;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * Abstract base class for message listener containers providing basic lifecycle
 * capabilities and collaborator for the concrete sub classes. This class implements all
 * lifecycle and configuration specific interface used by the Spring container to create,
 * initialize and start the container.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
abstract class AbstractMessageListenerContainer
		implements InitializingBean, DisposableBean, SmartLifecycle, BeanNameAware {

	private static final String RECEIVING_ATTRIBUTES = "All";

	private static final String RECEIVING_MESSAGE_ATTRIBUTES = "All";

	private static final int DEFAULT_MAX_NUMBER_OF_MESSAGES = 10;

	private static final int DEFAULT_WAIT_TIME_IN_SECONDS = 20;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Object lifecycleMonitor = new Object();

	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private final Map<String, QueueAttributes> registeredQueues = new HashMap<>();

	// Mandatory settings, the container synchronizes this fields after calling the
	// setters hence there is no further synchronization
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private AmazonSQSAsync amazonSqs;

	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private DestinationResolver<String> destinationResolver;

	private String beanName;

	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private QueueMessageHandler messageHandler;

	// Optional settings with no defaults
	private Integer maxNumberOfMessages;

	private Integer visibilityTimeout;

	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private ResourceIdResolver resourceIdResolver;

	/**
	 * By default sets the maximum value for long polling in SQS. For more information
	 * read the <a href=
	 * "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html">documentation</a>
	 */
	private Integer waitTimeOut = DEFAULT_WAIT_TIME_IN_SECONDS;

	// Optional settings with defaults
	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	// Settings that are changed at runtime
	private boolean active;

	private boolean running;

	protected Map<String, QueueAttributes> getRegisteredQueues() {
		return Collections.unmodifiableMap(this.registeredQueues);
	}

	protected QueueMessageHandler getMessageHandler() {
		return this.messageHandler;
	}

	public void setMessageHandler(QueueMessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	protected Object getLifecycleMonitor() {
		return this.lifecycleMonitor;
	}

	protected Logger getLogger() {
		return this.logger;
	}

	protected AmazonSQSAsync getAmazonSqs() {
		return this.amazonSqs;
	}

	/**
	 * Configures the mandatory {@link AmazonSQS} client for this instance.
	 * <b>Note:</b>The configured instance should have a buffering amazon SQS instance
	 * (see subclasses) functionality to improve the performance during message reception
	 * and deletion on the queueing system.
	 * @param amazonSqs the amazon sqs instance. Must not be null
	 */
	public void setAmazonSqs(AmazonSQSAsync amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	protected DestinationResolver<String> getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * Configures the destination resolver used to retrieve the queue url based on the
	 * destination name configured for this instance. <br/>
	 * This setter can be used when a custom configured {@link DestinationResolver} must
	 * be provided. (For example if one want to have the
	 * {@link DynamicQueueUrlDestinationResolver} with the auto creation of queues set to
	 * {@code true}.
	 * @param destinationResolver - the destination resolver. Must not be null
	 */
	public void setDestinationResolver(DestinationResolver<String> destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	protected String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected Integer getMaxNumberOfMessages() {
		return this.maxNumberOfMessages;
	}

	/**
	 * Configure the maximum number of messages that should be retrieved during one poll
	 * to the Amazon SQS system. This number must be a positive, non-zero number that has
	 * a maximum number of 10. Values higher then 10 are currently not supported by the
	 * queueing system.
	 * @param maxNumberOfMessages the maximum number of messages (between 1-10)
	 */
	public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
		this.maxNumberOfMessages = maxNumberOfMessages;
	}

	protected Integer getVisibilityTimeout() {
		return this.visibilityTimeout;
	}

	/**
	 * Configures the duration (in seconds) that the received messages are hidden from
	 * subsequent poll requests after being retrieved from the system.
	 * @param visibilityTimeout the visibility timeout in seconds
	 */
	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}

	/**
	 * This value must be set if no destination resolver has been set.
	 * @param resourceIdResolver the resourceIdResolver to use for resolving logical to
	 * physical ids in a CloudFormation environment. Must not be null.
	 */
	@RuntimeUse
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	protected Integer getWaitTimeOut() {
		return this.waitTimeOut;
	}

	/**
	 * Configures the wait timeout that the poll request will wait for new message to
	 * arrive if the are currently no messages on the queue. Higher values will reduce
	 * poll request to the system significantly.
	 * @param waitTimeOut - the wait time out in seconds
	 */
	public void setWaitTimeOut(Integer waitTimeOut) {
		this.waitTimeOut = waitTimeOut;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Configures if this container should be automatically started. The default value is
	 * true
	 * @param autoStartup - false if the container will be manually started
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Configure a custom phase for the container to start. This allows to start other
	 * beans that also implements the {@link SmartLifecycle} interface.
	 * @param phase - the phase that defines the phase respecting the
	 * {@link org.springframework.core.Ordered} semantics
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	public boolean isActive() {
		synchronized (this.getLifecycleMonitor()) {
			return this.active;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		validateConfiguration();
		initialize();
	}

	private void validateConfiguration() {
		Assert.state(this.amazonSqs != null, "amazonSqs must not be null");
		Assert.state(this.messageHandler != null, "messageHandler must not be null");
	}

	protected void initialize() {
		synchronized (this.getLifecycleMonitor()) {
			if (this.destinationResolver == null) {
				if (this.resourceIdResolver == null) {
					this.destinationResolver = new CachingDestinationResolverProxy<>(
							new DynamicQueueUrlDestinationResolver(this.amazonSqs));
				}
				else {
					this.destinationResolver = new CachingDestinationResolverProxy<>(
							new DynamicQueueUrlDestinationResolver(this.amazonSqs,
									this.resourceIdResolver));
				}
			}

			for (QueueMessageHandler.MappingInformation mappingInformation : this.messageHandler
					.getHandlerMethods().keySet()) {
				for (String queue : mappingInformation.getLogicalResourceIds()) {
					QueueAttributes queueAttributes = queueAttributes(queue,
							mappingInformation.getDeletionPolicy());

					if (queueAttributes != null) {
						this.registeredQueues.put(queue, queueAttributes);
					}
				}
			}

			this.active = true;
			this.getLifecycleMonitor().notifyAll();
		}
	}

	@Override
	public void start() {
		getLogger().debug("Starting container with name {}", getBeanName());
		synchronized (this.getLifecycleMonitor()) {
			this.running = true;
			this.getLifecycleMonitor().notifyAll();
		}
		doStart();
	}

	private QueueAttributes queueAttributes(String queue,
			SqsMessageDeletionPolicy deletionPolicy) {
		String destinationUrl;
		try {
			destinationUrl = getDestinationResolver().resolveDestination(queue);
		}
		catch (DestinationResolutionException e) {
			if (getLogger().isDebugEnabled()) {
				getLogger().debug(
						"Ignoring queue with name '" + queue + "': " + e.getMessage(), e);
			}
			else {
				getLogger().warn(
						"Ignoring queue with name '" + queue + "': " + e.getMessage());
			}
			return null;
		}

		GetQueueAttributesResult queueAttributes = getAmazonSqs()
				.getQueueAttributes(new GetQueueAttributesRequest(destinationUrl)
						.withAttributeNames(QueueAttributeName.RedrivePolicy));
		boolean hasRedrivePolicy = queueAttributes.getAttributes()
				.containsKey(QueueAttributeName.RedrivePolicy.toString());

		return new QueueAttributes(hasRedrivePolicy, deletionPolicy, destinationUrl,
				getMaxNumberOfMessages(), getVisibilityTimeout(), getWaitTimeOut());
	}

	@Override
	public void stop() {
		getLogger().debug("Stopping container with name {}", getBeanName());
		synchronized (this.getLifecycleMonitor()) {
			this.running = false;
			this.getLifecycleMonitor().notifyAll();
		}
		doStop();
	}

	@Override
	public boolean isRunning() {
		synchronized (this.getLifecycleMonitor()) {
			return this.running;
		}
	}

	@Override
	public void destroy() {
		synchronized (this.lifecycleMonitor) {
			stop();
			this.active = false;
			doDestroy();
		}
	}

	protected abstract void doStart();

	protected abstract void doStop();

	protected void doDestroy() {

	}

	protected static class QueueAttributes {

		private final boolean hasRedrivePolicy;

		private final SqsMessageDeletionPolicy deletionPolicy;

		private final String destinationUrl;

		private final Integer maxNumberOfMessages;

		private final Integer visibilityTimeout;

		private final Integer waitTimeOut;

		public QueueAttributes(boolean hasRedrivePolicy,
				SqsMessageDeletionPolicy deletionPolicy, String destinationUrl,
				Integer maxNumberOfMessages, Integer visibilityTimeout,
				Integer waitTimeOut) {
			this.hasRedrivePolicy = hasRedrivePolicy;
			this.deletionPolicy = deletionPolicy;
			this.destinationUrl = destinationUrl;
			this.maxNumberOfMessages = maxNumberOfMessages;
			this.visibilityTimeout = visibilityTimeout;
			this.waitTimeOut = waitTimeOut;
		}

		public boolean hasRedrivePolicy() {
			return this.hasRedrivePolicy;
		}

		public ReceiveMessageRequest getReceiveMessageRequest() {
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
					this.destinationUrl).withAttributeNames(RECEIVING_ATTRIBUTES)
							.withMessageAttributeNames(RECEIVING_MESSAGE_ATTRIBUTES);

			if (this.maxNumberOfMessages != null) {
				receiveMessageRequest.withMaxNumberOfMessages(this.maxNumberOfMessages);
			}
			else {
				receiveMessageRequest
						.withMaxNumberOfMessages(DEFAULT_MAX_NUMBER_OF_MESSAGES);
			}

			if (this.visibilityTimeout != null) {
				receiveMessageRequest.withVisibilityTimeout(this.visibilityTimeout);
			}

			if (this.waitTimeOut != null) {
				receiveMessageRequest.setWaitTimeSeconds(this.waitTimeOut);
			}

			return receiveMessageRequest;
		}

		public SqsMessageDeletionPolicy getDeletionPolicy() {
			return this.deletionPolicy;
		}

	}

}
