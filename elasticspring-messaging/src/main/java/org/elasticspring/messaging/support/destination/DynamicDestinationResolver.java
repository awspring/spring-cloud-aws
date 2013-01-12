/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support.destination;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

/**
 *
 */
public class DynamicDestinationResolver implements DestinationResolver {

	private final AmazonSQS queueingService;
	private boolean autoCreate = true;

	public DynamicDestinationResolver(AmazonSQS queueingService) {
		this.queueingService = queueingService;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestinationName(String destination) {

		if (this.autoCreate) {
			CreateQueueResult createQueueResult = this.getQueueingService().createQueue(new CreateQueueRequest(destination));
			return createQueueResult.getQueueUrl();
		} else {
			try {
				GetQueueUrlResult getQueueUrlResult = this.getQueueingService().getQueueUrl(new GetQueueUrlRequest(destination));
				return getQueueUrlResult.getQueueUrl();
			} catch (AmazonServiceException e) {
				if ("AWS.SimpleQueueService.NonExistentQueue".equals(e.getErrorCode())) {
					throw new InvalidDestinationException(destination);
				}
			}
		}
		return null;
	}

	public AmazonSQS getQueueingService() {
		return this.queueingService;
	}
}