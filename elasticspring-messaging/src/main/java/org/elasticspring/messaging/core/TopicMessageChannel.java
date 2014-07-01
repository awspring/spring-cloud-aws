/*
 * Copyright 2013-2014 the original author or authors.
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

package org.elasticspring.messaging.core;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class TopicMessageChannel implements MessageChannel {

	// TODO consider a method that sets the header with this key. For the moment the user needs to know this constant.
	public static final String NOTIFICATION_SUBJECT_HEADER = "NOTIFICATION_SUBJECT_HEADER";

	private final AmazonSNS amazonSns;
	private final String topicArn;

	public TopicMessageChannel(AmazonSNS amazonSns, String topicArn) {
		this.amazonSns = amazonSns;
		this.topicArn = topicArn;
	}

	@Override
	public boolean send(Message<?> message) {
		return this.send(message, 0);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		this.amazonSns.publish(new PublishRequest(this.topicArn,
				message.getPayload().toString(), findNotificationSubject(message)));
		return true;
	}

	private static String findNotificationSubject(Message<?> message) {
		return message.getHeaders().containsKey(NOTIFICATION_SUBJECT_HEADER) ? message.getHeaders().get(NOTIFICATION_SUBJECT_HEADER).toString() : null;
	}
}
