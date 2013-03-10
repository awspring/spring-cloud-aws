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
import com.amazonaws.services.sns.model.Topic;

import java.util.List;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicTopicDestinationResolver implements DestinationResolver {

	private final AmazonSNS amazonSNS;
	private boolean autoCreate;

	public DynamicTopicDestinationResolver(AmazonSNS amazonSNS) {
		this.amazonSNS = amazonSNS;
	}

	@Override
	public String resolveDestinationName(String destination) {
		if (destination.startsWith("arn")) {
			return destination;
		}
		if (this.autoCreate) {
			return this.amazonSNS.createTopic(new CreateTopicRequest(destination)).getTopicArn();
		} else {
			List<Topic> topics = this.amazonSNS.listTopics(new ListTopicsRequest(destination)).getTopics();
			if (topics.isEmpty()) {
				throw new IllegalArgumentException("No Topic with name: '" + destination + "' found. Please use " +
						"the right topic name or enable auto creation of topics for this DestinationResolver");
			}
			return topics.get(0).getTopicArn();
		}
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}
}