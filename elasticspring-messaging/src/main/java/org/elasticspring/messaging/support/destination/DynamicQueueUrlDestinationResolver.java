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

package org.elasticspring.messaging.support.destination;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import org.elasticspring.core.env.ResourceIdResolver;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicQueueUrlDestinationResolver implements DestinationResolver<String> {

	private final AmazonSQS amazonSqs;
	private final ResourceIdResolver resourceIdResolver;
	private boolean autoCreate;

	public DynamicQueueUrlDestinationResolver(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver) {
		this.amazonSqs = amazonSqs;
		this.resourceIdResolver = resourceIdResolver;
	}

	public DynamicQueueUrlDestinationResolver(AmazonSQS amazonSqs) {
		this(amazonSqs, null);
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestination(String name) throws DestinationResolutionException {
		//TODO: Consider util
		if (name.startsWith("http")) {
			return name;
		}

		if (this.resourceIdResolver != null) {
			String physicalResourceId = this.resourceIdResolver.resolveToPhysicalResourceId(name);
			if (!name.equals(physicalResourceId)) {
				// name was resolved otherwise it would be equal
				return physicalResourceId;
			}
		}

		if (this.autoCreate) {
			CreateQueueResult createQueueResult = this.amazonSqs.createQueue(new CreateQueueRequest(name));
			return createQueueResult.getQueueUrl();
		} else {
			try {
				GetQueueUrlResult getQueueUrlResult = this.amazonSqs.getQueueUrl(new GetQueueUrlRequest(name));
				return getQueueUrlResult.getQueueUrl();
			} catch (AmazonServiceException e) {
				//TODO: Check for exception subclass
				//TODO: Consider auto.creating only if the queue does not exist and autocreate is true
				if ("AWS.SimpleQueueService.NonExistentQueue".equals(e.getErrorCode())) {
					throw new InvalidDestinationException(name);
				} else {
					throw e;
				}
			}
		}
	}
}
