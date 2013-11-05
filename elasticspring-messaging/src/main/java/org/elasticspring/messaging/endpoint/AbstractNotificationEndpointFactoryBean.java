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
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicTopicDestinationResolver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract {@link org.springframework.beans.factory.FactoryBean} implementation to create notification endpoint which
 * will receive notification messages. This base class contains the base functionality used by the concrete and
 * protocol specific sub classes.
 * <p>Subclasses will typically create protocol specific endpoints which will be notified through the particular
 * protocol</p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
abstract class AbstractNotificationEndpointFactoryBean<T> extends AbstractFactoryBean<T> {

	/**
	 * The {@link AmazonSNS} client used to query data from the Amazon SNS service
	 */
	private final AmazonSNS amazonSns;

	/**
	 * The logical (or physical) topic name that the endpoint points to.
	 */
	private final String topicName;

	/**
	 * The protocol configured for the endpoint
	 */
	private final TopicListener.NotificationProtocol protocol;

	/**
	 * The protocol specific endpoint address (e.g. the http/s url)
	 */
	private final String endpoint;

	/**
	 * The target which will be effectively called by the endpoint
	 */
	private final Object target;

	/**
	 * The method name on the target that will be called
	 */
	private final String method;

	/**
	 * The destination resolver used to resolve topic arn based on the logical name
	 */
	private DestinationResolver<MessageChannel> destinationResolver;

	/**
	 * Constructs this base class with all collaborators and configuration information. This constructor creates and uses
	 * a
	 * {@link DynamicTopicDestinationResolver} which will be used to resolve the topic arn based on the logical topic
	 * name.
	 *
	 * @param amazonSns
	 * 		- the Amazon SNS client used, must not be null
	 * @param topicName
	 * 		- the topic name which can be the logical topic name (e.g. "myTopic") or the topic arn which is the fully
	 * 		qualified name following the amazon resource notation.
	 * @param protocol
	 * 		- the protocol for which the endpoint will be configured. This class will only check if there is a valid
	 * 		subscription based on the protocol and subscription available, but will not execute any protocol specific
	 * 		behaviour. The value must not be null
	 * @param endpoint
	 * 		- the endpoint address for this endpoint. The endpoint address must match to a valid subscription. The value must
	 * 		not be null
	 * @param target
	 * 		- the target bean which will be called by the endpoint. The bean could by of any type as long it is accessible.
	 * 		Must not be null
	 * @param method
	 * 		- the method name that will be called by the endpoint. The method must exist and be accessible on the target
	 * 		object. Must not be null
	 * @throws IllegalArgumentException
	 * 		if one of the mandatory parameters is null
	 */
	AbstractNotificationEndpointFactoryBean(AmazonSNS amazonSns, String topicName, TopicListener.NotificationProtocol protocol, String endpoint, Object target, String method) {
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
		this.destinationResolver = new CachingDestinationResolver<MessageChannel>(new DynamicTopicDestinationResolver(amazonSns));
	}

	/**
	 * Configures an alternative {@link DestinationResolver} to be used by this instance to actually retrieve the topic
	 * arn
	 * based on the logical name configured for this endpoint. By default, this classes uses a {@link
	 * DynamicTopicDestinationResolver} in combination with a {@link CachingDestinationResolver} to resolve the names with
	 * a minimum performance overhead.
	 *
	 * @param destinationResolver
	 * 		the destination resolver, must not be null
	 */
	public void setDestinationResolver(DestinationResolver<MessageChannel> destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Create the instance by resolving the topic arn and retrieving the subscription for the configured endpoint
	 * information. Delegates to {@link #doCreateEndpointInstance(com.amazonaws.services.sns.model.Subscription)} to
	 * actually create the instance by the particular sub class.
	 *
	 * @return the create endpoint instance
	 * @throws IllegalArgumentException
	 * 		if there is no endpoint or subscription configured for the endpoint information
	 */
	@Override
	protected T createInstance() throws Exception {
		String topicArn =  getTopicResourceName(null, getTopicName());
		Subscription subscriptionForEndpoint = getSubscriptionForEndpoint(topicArn, null);
		return doCreateEndpointInstance(subscriptionForEndpoint);
	}

	/**
	 * Template method called by {@link #createInstance()} to actually create the instance by the underlying subclass.
	 *
	 * @param subscription
	 * 		- the subscription from the Amazon SNS service containing subscription and topic information
	 * @return the fully initialized endpoint instance.
	 */
	protected abstract T doCreateEndpointInstance(Subscription subscription);

	/**
	 * Retrieves the subscription for the topic arn, will be called recursively in case of a token which implies that
	 * there are more subscription available for a topic. This method retrieves all subscriptions for the endpoint and
	 * compares them if they match based on the protocol and the endpoint.
	 *
	 * @param topicArn
	 * 		- the topic arn used to get the subscription
	 * @param nextToken
	 * 		- the token if the method should fetch the subscription information with the token. Can be null if there is no
	 * 		token active (e.g. the first request)
	 * @return the subscription for the topic arn matching the configured protocol and endpoint.
	 * @throws IllegalArgumentException
	 * 		if there is no matching subscription available for the endpoint.
	 */
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

	/**
	 * Delegates to the parent method and initializes the bean if the create bean is of type {@link InitializingBean}
	 */
	@Override
	public final void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (getObject() instanceof InitializingBean) {
			((InitializingBean) getObject()).afterPropertiesSet();
		}
	}

	/**
	 * Destroys the instance if the instance itself implement {@link DisposableBean}
	 *
	 * @param instance
	 * 		the instance which has been created by {@link #createInstance()}
	 * @throws Exception
	 */
	@Override
	protected final void destroyInstance(Object instance) throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) instance).destroy();
		}
	}

	/**
	 * Provides the amazon SNS client for sub classes. Might be overwritten in case of a custom lookup logic.
	 *
	 * @return the amazon sns client configured in the constructor
	 */
	protected AmazonSNS getAmazonSns() {
		return this.amazonSns;
	}

	/**
	 * Returns the configured topic name (not necessarily the topic arn!) for this endpoint.
	 *
	 * @return the configured endpoint in the constructor
	 */
	protected String getTopicName() {
		return this.topicName;
	}

	/**
	 * Return the configured protocol for this endpoint
	 *
	 * @return the configured protocol used in the constructor
	 */
	protected TopicListener.NotificationProtocol getProtocol() {
		return this.protocol;
	}

	/**
	 * Provides the target configured for this endpoint
	 *
	 * @return the configured target in the endpoint
	 */
	protected Object getTarget() {
		return this.target;
	}

	/**
	 * Return the method name that should be called on the target
	 *
	 * @return the configured method name in the constructor
	 */
	protected String getMethod() {
		return this.method;
	}

	/**
	 * Return the configured endpoint (address) or this endpoint.
	 *
	 * @return the configured endpoint in the constructor
	 */
	protected String getEndpoint() {
		return this.endpoint;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private String getTopicResourceName(String marker, String topicName) {
			ListTopicsResult listTopicsResult = this.amazonSns.listTopics(new ListTopicsRequest(marker));
			for (Topic topic : listTopicsResult.getTopics()) {
				if (AmazonResourceName.isValidAmazonResourceName(topicName)) {
					if (topic.getTopicArn().equals(topicName)) {
						return topic.getTopicArn();
					}
				} else {
					AmazonResourceName resourceName = AmazonResourceName.fromString(topic.getTopicArn());
					if (resourceName.getResourceType().equals(topicName)) {
						return topic.getTopicArn();
					}
				}
			}

			if (StringUtils.hasText(listTopicsResult.getNextToken())) {
				return getTopicResourceName(listTopicsResult.getNextToken(), topicName);
			} else {
				throw new IllegalArgumentException("No topic found for name :'" + topicName + "'");
			}
		}
}