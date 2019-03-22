/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.support.destination;

import java.net.URI;
import java.net.URISyntaxException;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicQueueUrlDestinationResolver implements DestinationResolver<String> {

	private final AmazonSQS amazonSqs;

	private final ResourceIdResolver resourceIdResolver;

	private boolean autoCreate;

	public DynamicQueueUrlDestinationResolver(AmazonSQS amazonSqs,
			ResourceIdResolver resourceIdResolver) {
		Assert.notNull(amazonSqs, "amazonSqs must not be null");

		this.amazonSqs = amazonSqs;
		this.resourceIdResolver = resourceIdResolver;
	}

	public DynamicQueueUrlDestinationResolver(AmazonSQS amazonSqs) {
		this(amazonSqs, null);
	}

	private static boolean isValidQueueUrl(String name) {
		try {
			URI candidate = new URI(name);
			return ("http".equals(candidate.getScheme())
					|| "https".equals(candidate.getScheme()));
		}
		catch (URISyntaxException e) {
			return false;
		}
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestination(String name) throws DestinationResolutionException {
		String queueName = name;

		if (this.resourceIdResolver != null) {
			queueName = this.resourceIdResolver.resolveToPhysicalResourceId(name);
		}

		if (isValidQueueUrl(queueName)) {
			return queueName;
		}

		if (this.autoCreate) {
			// Auto-create is fine to be called even if the queue exists.
			CreateQueueResult createQueueResult = this.amazonSqs
					.createQueue(new CreateQueueRequest(queueName));
			return createQueueResult.getQueueUrl();
		}
		else {
			try {
				GetQueueUrlResult getQueueUrlResult = this.amazonSqs
						.getQueueUrl(new GetQueueUrlRequest(queueName));
				return getQueueUrlResult.getQueueUrl();
			}
			catch (QueueDoesNotExistException e) {
				throw toDestinationResolutionException(e);
			}
		}
	}

	private DestinationResolutionException toDestinationResolutionException(
			QueueDoesNotExistException e) {
		if (e.getMessage() != null && e.getMessage().contains("access")) {
			return new DestinationResolutionException(
					"The queue does not exist or no access to perform action sqs:GetQueueUrl.",
					e);
		}
		else {
			return new DestinationResolutionException("The queue does not exist.", e);
		}
	}

}
