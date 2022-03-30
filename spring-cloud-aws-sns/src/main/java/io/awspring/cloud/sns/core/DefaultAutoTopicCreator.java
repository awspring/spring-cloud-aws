/*
 * Copyright 2013-2022 the original author or authors.
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

package io.awspring.cloud.sns.core;

import software.amazon.awssdk.services.sns.SnsClient;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link AutoTopicCreator} used to determine topic ARN by name
 * and create a topic. If AutoCreate is turned off destination must be ARN, meaning topic
 * ARN can't be resolved by topic name.
 *
 * @author Matej Nedic
 */
public class DefaultAutoTopicCreator implements AutoTopicCreator {

	private final SnsClient snsClient;

	private final boolean autoCreate;

	public DefaultAutoTopicCreator(SnsClient snsClient, boolean autoCreate) {
		this.snsClient = snsClient;
		this.autoCreate = autoCreate;
	}

	public DefaultAutoTopicCreator(SnsClient snsClient) {
		this(snsClient, true);
	}

	/**
	 * AutoCreate must be specified with true if topics ARN is to be found or if topic is
	 * to be created by name. If AutoCreate is turned off method should only accept ARN.
	 */
	public String createTopicBasedOnName(String destination) {
		Assert.notNull(destination, "Destination must not be null");
		if (this.autoCreate && !destination.toLowerCase().startsWith("arn:")) {
			return this.snsClient.createTopic(request -> request.name(destination)).topicArn();
		}
		else {
			Assert.isTrue(destination.toLowerCase().startsWith("arn"),
					"Only sending notifications which has destination as an ARN is allowed when AutoCreate is turned off.");
			return destination;
		}
	}

}
