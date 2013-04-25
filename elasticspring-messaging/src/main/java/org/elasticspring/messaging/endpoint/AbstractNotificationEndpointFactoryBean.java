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

package org.elasticspring.messaging.endpoint;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import org.elasticspring.core.naming.AmazonResourceName;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public abstract class AbstractNotificationEndpointFactoryBean<T> extends AbstractFactoryBean<T> {

	private final AmazonSNS amazonSns;
	private final String topicName;
	private final TopicListener.NotificationProtocol protocol;
	private final String endpoint;
	private final Object target;
	private final String method;

	public AbstractNotificationEndpointFactoryBean(AmazonSNS amazonSns, String topicName, TopicListener.NotificationProtocol protocol, String endpoint, Object target, String method) {
		Assert.notNull(amazonSns, "amazonSns must not be null");
		Assert.notNull(topicName, "topicName must not be null");
		Assert.notNull(protocol, "protocol must not be null");
		Assert.notNull(endpoint, "endpoint must not be null");
		Assert.notNull(method, "method must not be null");
		this.amazonSns = amazonSns;
		this.topicName = topicName;
		this.protocol = protocol;
		this.endpoint = endpoint;
		this.target = target;
		this.method = method;
	}

	@Override
	protected T createInstance() throws Exception {
		String topicArn = getTopicResourceName(null);
		Subscription subscriptionForEndpoint = getSubscriptionForEndpoint(topicArn, null);
		return doCreateEndpointInstance(subscriptionForEndpoint);
	}

	protected abstract T doCreateEndpointInstance(Subscription subscription);

	private String getTopicResourceName(String marker) {
		ListTopicsResult listTopicsResult = this.amazonSns.listTopics(new ListTopicsRequest(marker));
		for (Topic topic : listTopicsResult.getTopics()) {
			if (AmazonResourceName.isValidAmazonResourceName(getTopicName())) {
				if (topic.getTopicArn().equals(getTopicName())) {
					return topic.getTopicArn();
				}
			} else {
				AmazonResourceName resourceName = AmazonResourceName.fromString(topic.getTopicArn());
				if (resourceName.getResourceType().equals(getTopicName())) {
					return topic.getTopicArn();
				}
			}
		}

		if (StringUtils.hasText(listTopicsResult.getNextToken())) {
			return getTopicResourceName(listTopicsResult.getNextToken());
		} else {
			throw new IllegalArgumentException("No topic found for name :'" + this.topicName + "'");
		}
	}

	private Subscription getSubscriptionForEndpoint(String topicArn, String nextToken) {

		ListSubscriptionsByTopicResult listSubscriptionsByTopicResult = getAmazonSns().listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn, nextToken));
		for (Subscription subscription : listSubscriptionsByTopicResult.getSubscriptions()) {
			if (subscription.getProtocol().equals(getProtocol().getCanonicalName()) &&
					subscription.getEndpoint().equals(getEndpoint())) {
				return subscription;
			}
		}

		if (StringUtils.hasText(listSubscriptionsByTopicResult.getNextToken())) {
			return getSubscriptionForEndpoint(topicArn, listSubscriptionsByTopicResult.getNextToken());
		} else {
			throw new IllegalArgumentException("No subscription found for topic arn:'" + topicArn + "' and endpoint:'" + getEndpoint() + "'");
		}
	}


	@Override
	public final void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (getObject() instanceof InitializingBean) {
			((InitializingBean) getObject()).afterPropertiesSet();
		}
	}

	@Override
	protected final void destroyInstance(Object instance) throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) instance).destroy();
		}
	}

	protected AmazonSNS getAmazonSns() {
		return this.amazonSns;
	}

	protected String getTopicName() {
		return this.topicName;
	}

	protected TopicListener.NotificationProtocol getProtocol() {
		return this.protocol;
	}

	protected Object getTarget() {
		return this.target;
	}

	protected String getMethod() {
		return this.method;
	}

	protected String getEndpoint() {
		return this.endpoint;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
