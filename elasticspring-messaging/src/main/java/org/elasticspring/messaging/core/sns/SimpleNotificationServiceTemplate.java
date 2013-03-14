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

package org.elasticspring.messaging.core.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import org.elasticspring.messaging.Message;
import org.elasticspring.messaging.core.NotificationOperations;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.converter.StringMessageConverter;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicTopicDestinationResolver;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleNotificationServiceTemplate implements NotificationOperations {

	private final AmazonSNS amazonSNS;
	private MessageConverter messageConverter = new StringMessageConverter();
	private DestinationResolver destinationResolver;
	private String defaultDestinationName;

	public SimpleNotificationServiceTemplate(AmazonSNS amazonSNS) {
		Assert.notNull(amazonSNS, "amazonSNS must not be null");
		this.amazonSNS = amazonSNS;
		this.destinationResolver = new CachingDestinationResolver(new DynamicTopicDestinationResolver(amazonSNS));
	}

	public void setDefaultDestinationName(String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	public void convertAndSend(Object payload) {
		Assert.isTrue(this.defaultDestinationName != null, "No default destination name configured for this template.");
		this.convertAndSend(this.defaultDestinationName, payload);
	}

	@Override
	public void convertAndSendWithSubject(Object payload, String subject) {
		this.convertAndSendWithSubject(this.defaultDestinationName, payload, subject);
	}

	@Override
	public void convertAndSend(String destinationName, Object payload) {
		this.convertAndSendWithSubject(destinationName, payload, null);
	}

	@Override
	public void convertAndSendWithSubject(String destinationName, Object payload, String subject) {
		String topicArn = this.destinationResolver.resolveDestinationName(destinationName);
		Message<String> message = this.messageConverter.toMessage(payload);

		if (subject == null) {
			this.amazonSNS.publish(new PublishRequest(topicArn, message.getPayload()));
		} else {
			this.amazonSNS.publish(new PublishRequest(topicArn, message.getPayload(), subject));
		}
	}

}
