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

package io.awspring.cloud.v3.messaging.support.destination;

import java.net.URI;
import java.net.URISyntaxException;

import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicQueueUrlDestinationResolver implements DestinationResolver<String> {

	private final SqsClient amazonSqs;

	private final ResourceIdResolver resourceIdResolver;

	private boolean autoCreate;

	public DynamicQueueUrlDestinationResolver(SqsClient amazonSqs, ResourceIdResolver resourceIdResolver) {
		Assert.notNull(amazonSqs, "amazonSqs must not be null");

		this.amazonSqs = amazonSqs;
		this.resourceIdResolver = resourceIdResolver;
	}

	public DynamicQueueUrlDestinationResolver(SqsClient amazonSqs) {
		this(amazonSqs, null);
	}

	private static boolean isValidQueueUrl(String name) {
		try {
			URI candidate = new URI(name);
			return ("http".equals(candidate.getScheme()) || "https".equals(candidate.getScheme()));
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
			CreateQueueResponse createQueueResponse = this.amazonSqs.createQueue(CreateQueueRequest.builder()
				.queueName(queueName)
				.build());
			return createQueueResponse.queueUrl();
		}
		else {
			try {
				GetQueueUrlResponse queueUrlResponse = this.amazonSqs.getQueueUrl(GetQueueUrlRequest.builder()
					.queueName(queueName)
					.build());
				return queueUrlResponse.queueUrl();
			}
			catch (QueueDoesNotExistException e) {
				throw toDestinationResolutionException(e);
			}
		}
	}

	private DestinationResolutionException toDestinationResolutionException(QueueDoesNotExistException e) {
		if (e.getMessage() != null && e.getMessage().contains("access")) {
			return new DestinationResolutionException(
					"The queue does not exist or no access to perform action sqs:GetQueueUrl.", e);
		}
		else {
			return new DestinationResolutionException("The queue does not exist.", e);
		}
	}

}
