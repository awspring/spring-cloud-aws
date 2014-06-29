/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.listener;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.elasticspring.core.env.ResourceIdResolver;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for message listener containers providing basic lifecycle capabilities and collaborator for the
 * concrete sub classes. This class implements all lifecycle and configuration specific interface used by the Spring
 * container to create, initialize and start the container.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
abstract class AbstractMessageListenerContainer implements InitializingBean, DisposableBean, SmartLifecycle, BeanNameAware {

	private static final String MESSAGE_RECEIVING_ATTRIBUTE_NAMES = "All";
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Object lifecycleMonitor = new Object();

	//Mandatory settings, the container synchronizes this fields after calling the setters hence there is no further synchronization
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private AmazonSQS amazonSqs;
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private DestinationResolver<String> destinationResolver;
	private String beanName;
	private final Set<String> queues = new HashSet<String>();
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private QueueMessageHandler messageHandler;

	//Optional settings with no defaults
	private Integer maxNumberOfMessages;
	private Integer visibilityTimeout;
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private ResourceIdResolver resourceIdResolver;

	private Integer waitTimeOut;
	//Optional settings with defaults
	private boolean autoStartup = true;
	private int phase = Integer.MAX_VALUE;

	private ErrorHandler errorHandler = new LoggingErrorHandler();
	//Settings that are changed at runtime
	private boolean active;
	private boolean running;
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private final Map<String, ReceiveMessageRequest> messageRequests = new HashMap<String, ReceiveMessageRequest>();

	public Map<String, ReceiveMessageRequest> getMessageRequests() {
		return Collections.unmodifiableMap(this.messageRequests);
	}

	public QueueMessageHandler getMessageHandler() {
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

	protected AmazonSQS getAmazonSqs() {
		return this.amazonSqs;
	}

	/**
	 * Configures the mandatory {@link AmazonSQS} client for this instance.
	 * <b>Note:</b>The configured instance should have a buffering amazon SQS instance (see subclasses) functionality
	 * to
	 * improve the performance during message reception and deletion on the queueing system.
	 *
	 * @param amazonSqs
	 * 		the amazon sqs instance. Must not be null
	 */
	public void setAmazonSqs(AmazonSQS amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	protected DestinationResolver<String> getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * Configures the destination resolver used to retrieve the queue url based on the destination name configured for
	 * this instance.
	 *
	 * @param destinationResolver
	 * 		- the destination resolver. Must not be null
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
	 * Configure the maximum number of messages that should be retrieved during one poll to the Amazon SQS system. This
	 * number must be a positive, non-zero number that has a maximum number of 10. Values higher then 10 are currently
	 * not supported by the queueing system.
	 *
	 * @param maxNumberOfMessages
	 * 		the maximum number of messages (between 1-10)
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
	 *
	 * @param visibilityTimeout
	 * 		the visibility timeout in seconds
	 */
	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}

	/**
	 * This value must be set if no destination resolver has been set.
	 *
	 * @param resourceIdResolver
	 * 		the resourceIdResolver to use for resolving logical to physical ids in a CloudFormation environment.
	 * 		Must not be null.
	 */
	@RuntimeUse
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	protected Integer getWaitTimeOut() {
		return this.waitTimeOut;
	}

	/**
	 * Configures the wait timeout that the poll request will wait for new message to arrive if the are currently no
	 * messages on the queue. Higher values will reduce poll request to the system significantly.
	 *
	 * @param waitTimeOut
	 * 		- the wait time out in seconds
	 */
	public void setWaitTimeOut(Integer waitTimeOut) {
		this.waitTimeOut = waitTimeOut;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Configures if this container should be automatically started. The default value is true
	 *
	 * @param autoStartup
	 * 		- false if the container will be manually started
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Configure a custom phase for the container to start. This allows to start other beans that also implements the
	 * {@link SmartLifecycle} interface.
	 *
	 * @param phase
	 * 		- the phase that defines the phase respecting the {@link org.springframework.core.Ordered} semantics
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	protected ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * A custom error handler that will be called in case of any message listener error.
	 *
	 * @param errorHandler
	 * 		custom error handler implementation
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
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

	protected void validateConfiguration() {
		Assert.state(this.amazonSqs != null, "amazonSqs must not be null");
		Assert.state(this.messageHandler != null, "messageHandler must not be null");
	}

	protected void initialize() {
		synchronized (this.getLifecycleMonitor()) {
			if (this.destinationResolver == null) {
				if (this.resourceIdResolver == null) {
					this.destinationResolver = new CachingDestinationResolver<String>(new DynamicQueueUrlDestinationResolver(this.amazonSqs));
				} else {
					this.destinationResolver = new CachingDestinationResolver<String>(new DynamicQueueUrlDestinationResolver(this.amazonSqs, this.resourceIdResolver));
				}
			}

			this.active = true;
			this.getLifecycleMonitor().notifyAll();

			registerQueues();
		}
	}

	private void registerQueues() {
		for (QueueMessageHandler.MappingInformation mapping : this.messageHandler.getHandlerMethods().keySet()) {
			Set<String> queueNames = mapping.getLogicalResourceIds();
			for (String queueName : queueNames) {
				Assert.state(!this.queues.contains(queueName), "Queue name can only be mapped on one method");
				this.queues.add(queueName);
			}
		}
	}

	@Override
	public void start() {
		getLogger().debug("Starting container with name {}", getBeanName());
		synchronized (this.getLifecycleMonitor()) {
			for (String queue : this.queues) {
				String destinationUrl = getDestinationResolver().resolveDestination(queue);
				ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(destinationUrl).withAttributeNames(MESSAGE_RECEIVING_ATTRIBUTE_NAMES);
				if (getMaxNumberOfMessages() != null) {
					receiveMessageRequest.withMaxNumberOfMessages(getMaxNumberOfMessages());
				}

				if (getVisibilityTimeout() != null) {
					receiveMessageRequest.withVisibilityTimeout(getVisibilityTimeout());
				}

				if (getWaitTimeOut() != null) {
					receiveMessageRequest.setWaitTimeSeconds(getWaitTimeOut());
				}
				this.messageRequests.put(queue, receiveMessageRequest);
			}

			this.running = true;
			this.getLifecycleMonitor().notifyAll();
		}
		doStart();
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
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
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

	protected void handleError(Throwable throwable) {
		if (getErrorHandler() != null) {
			getErrorHandler().handleError(throwable);
		}
	}

	private class LoggingErrorHandler implements ErrorHandler {

		@Override
		public void handleError(Throwable t) {
			getLogger().error("Error occurred while processing message", t);
		}
	}
}