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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicQueueUrlDestinationResolver implements DestinationResolver<String> {

	private final AmazonSQS queueingService;
	private boolean autoCreate;

	public DynamicQueueUrlDestinationResolver(AmazonSQS queueingService) {
		this.queueingService = queueingService;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestination(String name) throws DestinationResolutionException {
		if (name.startsWith("http")) {
			return name;
		}

		if (this.autoCreate) {
			CreateQueueResult createQueueResult = this.queueingService.createQueue(new CreateQueueRequest(name));
			return createQueueResult.getQueueUrl();
		} else {
			try {
				GetQueueUrlResult getQueueUrlResult = this.queueingService.getQueueUrl(new GetQueueUrlRequest(name));
				return getQueueUrlResult.getQueueUrl();
			} catch (AmazonServiceException e) {
				if ("AWS.SimpleQueueService.NonExistentQueue".equals(e.getErrorCode())) {
					throw new InvalidDestinationException(name);
				} else {
					throw e;
				}
			}
		}
	}
}
