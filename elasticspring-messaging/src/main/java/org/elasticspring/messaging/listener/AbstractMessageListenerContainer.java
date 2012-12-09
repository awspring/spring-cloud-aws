/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.messaging.listener;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;

/**
 *
 */
public abstract class AbstractMessageListenerContainer implements InitializingBean, SmartLifecycle, BeanNameAware {

	private DestinationResolver destinationResolver;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private String beanName;

	private MessageListener messageListener;

	private String destinationName;

	private Integer maxNumberOfMessages;

	private Integer visibilityTimeout;

	private AmazonSQSAsync amazonSQS;

	private boolean active;

	private boolean running;

	private final Object lifecycleMonitor = new Object();

	private ReceiveMessageRequest receiveMessageRequest;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Integer waitTimeOut;

	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public MessageListener getMessageListener() {
		return this.messageListener;
	}

	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	public String getDestinationName() {
		return this.destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public Integer getMaxNumberOfMessages() {
		return this.maxNumberOfMessages;
	}

	public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
		this.maxNumberOfMessages = maxNumberOfMessages;
	}

	public Integer getVisibilityTimeout() {
		return this.visibilityTimeout;
	}

	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}

	public AmazonSQSAsync getAmazonSQS() {
		return this.amazonSQS;
	}

	public void setAmazonSQS(AmazonSQSAsync amazonSQS) {
		this.amazonSQS = amazonSQS;
	}

	public boolean isActive() {
		synchronized (this.getLifecycleMonitor()) {
			return this.active;
		}
	}

	public ReceiveMessageRequest getReceiveMessageRequest() {
		synchronized (this.getLifecycleMonitor()) {
			return this.receiveMessageRequest;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		synchronized (this.getLifecycleMonitor()) {
			this.active = true;
			this.getLifecycleMonitor().notifyAll();

			String destinationUrl = getDestinationResolver().resolveDestinationName(getDestinationName());
			ReceiveMessageRequest request = new ReceiveMessageRequest(destinationUrl).withAttributeNames("All");
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
		}
	}

	@Override
	public void start() {
		synchronized (this.getLifecycleMonitor()) {
			this.running = true;
			this.getLifecycleMonitor().notifyAll();
		}
		doStart();
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop() {
		synchronized (this.getLifecycleMonitor()) {
			this.running = false;
			this.getLifecycleMonitor().notifyAll();
		}
		doStop();
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		synchronized (this.getLifecycleMonitor()) {
			return this.running;
		}
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	protected abstract void doStart();

	protected abstract void doStop();

	protected Object getLifecycleMonitor() {
		return this.lifecycleMonitor;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public Integer getWaitTimeOut() {
		return this.waitTimeOut;
	}

	public void setWaitTimeOut(Integer waitTimeOut) {
		this.waitTimeOut = waitTimeOut;
	}
}