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

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueDestinationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @since 1.0
 */
abstract class AbstractMessageListenerContainer implements InitializingBean, DisposableBean, SmartLifecycle, BeanNameAware {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Object lifecycleMonitor = new Object();

	//Mandatory settings, the container synchronizes this fields after calling the setters hence there is no further synchronization
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private AmazonSQSAsync amazonSqs;
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private MessageListener messageListener;
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private String destinationName;
	@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
	private DestinationResolver destinationResolver;
	private String beanName;

	//Optional settings with no defaults
	private Integer maxNumberOfMessages;
	private Integer visibilityTimeout;
	private Integer waitTimeOut;

	//Optional settings with defaults
	private boolean autoStartup = true;
	private int phase = Integer.MAX_VALUE;

	//Settings that are changed at runtime
	private boolean active;
	private boolean running;

	private ReceiveMessageRequest receiveMessageRequest;

	protected Object getLifecycleMonitor() {
		return this.lifecycleMonitor;
	}

	protected Logger getLogger() {
		return this.logger;
	}

	protected AmazonSQSAsync getAmazonSqs() {
		return this.amazonSqs;
	}

	public void setAmazonSqs(AmazonSQSAsync amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	protected MessageListener getMessageListener() {
		return this.messageListener;
	}

	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	protected String getDestinationName() {
		return this.destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	protected DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
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

	public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
		this.maxNumberOfMessages = maxNumberOfMessages;
	}

	protected Integer getVisibilityTimeout() {
		return this.visibilityTimeout;
	}

	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}

	protected Integer getWaitTimeOut() {
		return this.waitTimeOut;
	}

	public void setWaitTimeOut(Integer waitTimeOut) {
		this.waitTimeOut = waitTimeOut;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public boolean isActive() {
		synchronized (this.getLifecycleMonitor()) {
			return this.active;
		}
	}

	protected ReceiveMessageRequest getReceiveMessageRequest() {
		synchronized (this.getLifecycleMonitor()) {
			return this.receiveMessageRequest;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		validateConfiguration();
		initialize();
	}

	protected void validateConfiguration() {
		Assert.state(this.amazonSqs != null, "amazonSqs must not be null");
		Assert.state(this.messageListener != null, "messageListener must not be null");
		Assert.state(this.destinationName != null, "destinationName must not be null");
	}

	protected void initialize() {
		synchronized (this.getLifecycleMonitor()) {
			if (this.destinationResolver == null) {
				this.destinationResolver = new CachingDestinationResolver(new DynamicQueueDestinationResolver(this.amazonSqs));
			}

			this.active = true;
			this.getLifecycleMonitor().notifyAll();
		}
	}

	@Override
	public void start() {
		getLogger().debug("Starting container with name {}", getBeanName());
		synchronized (this.getLifecycleMonitor()) {
			String destinationUrl = getDestinationResolver().resolveDestinationName(getDestinationName());
			ReceiveMessageRequest request = new ReceiveMessageRequest(destinationUrl);
			if (getMaxNumberOfMessages() != null) {
				request.withMaxNumberOfMessages(getMaxNumberOfMessages());
			}

			if (getVisibilityTimeout() != null) {
				request.withVisibilityTimeout(getVisibilityTimeout());
			}

			if (getWaitTimeOut() != null) {
				request.setWaitTimeSeconds(getWaitTimeOut());
			}

			this.receiveMessageRequest = request;


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
		}
	}

	protected abstract void doStart();

	protected abstract void doStop();
}