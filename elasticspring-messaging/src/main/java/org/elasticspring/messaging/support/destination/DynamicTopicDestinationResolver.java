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

package org.elasticspring.messaging.support.destination;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import org.elasticspring.core.naming.AmazonResourceName;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicTopicDestinationResolver implements DestinationResolver {

	private final AmazonSNS amazonSns;
	private boolean autoCreate;

	public DynamicTopicDestinationResolver(AmazonSNS amazonSNS) {
		this.amazonSns = amazonSNS;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestinationName(String destination) {
		if (this.autoCreate) {
			return this.amazonSns.createTopic(new CreateTopicRequest(destination)).getTopicArn();
		} else {
			String topicArn = getTopicResourceName(null, destination);
			if (topicArn == null) {
				throw new IllegalArgumentException("No Topic with name: '" + destination + "' found. Please use " +
						"the right topic name or enable auto creation of topics for this DestinationResolver");
			}
			return topicArn;
		}
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