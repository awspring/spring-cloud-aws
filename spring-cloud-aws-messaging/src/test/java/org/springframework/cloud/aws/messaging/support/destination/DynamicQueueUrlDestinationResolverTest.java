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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.junit.Test;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.messaging.core.DestinationResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicQueueUrlDestinationResolverTest {

	@Test
	public void testAutoCreate() throws Exception {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		String queueUrl = "https://foo/bar";
		when(amazonSqs.createQueue(new CreateQueueRequest("foo")))
				.thenReturn(new CreateQueueResult().withQueueUrl(queueUrl));

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(true);
		assertThat(dynamicQueueDestinationResolver.resolveDestination("foo"))
				.isEqualTo(queueUrl);
	}

	@Test
	public void testAbsoluteUrl() throws Exception {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		String destination = "https://sqs-amazon.aws.com/123123123/myQueue";
		assertThat(dynamicQueueDestinationResolver.resolveDestination(destination))
				.isEqualTo(destination);
	}

	@Test
	public void testNoAutoCreate() throws Exception {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		String queueUrl = "https://foo/bar";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest("foo")))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		assertThat(dynamicQueueDestinationResolver.resolveDestination("foo"))
				.isEqualTo(queueUrl);
	}

	@Test
	public void testInvalidDestinationName() throws Exception {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		AmazonServiceException exception = new QueueDoesNotExistException(
				"AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorCode("AWS.SimpleQueueService.NonExistentQueue");
		String queueUrl = "invalidName";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(queueUrl)))
				.thenThrow(exception);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		try {
			dynamicQueueDestinationResolver.resolveDestination(queueUrl);
		}
		catch (DestinationResolutionException e) {
			assertThat(e.getMessage()).startsWith("The queue does not exist.");
		}
	}

	@Test
	public void testPotentiallyNoAccessToPerformGetQueueUrl() throws Exception {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		AmazonServiceException exception = new QueueDoesNotExistException(
				"AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorCode("AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorMessage(
				"The specified queue does not exist or you do not have access to it.");
		String queueUrl = "noAccessGetQueueUrlName";
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(queueUrl)))
				.thenThrow(exception);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs);
		try {
			dynamicQueueDestinationResolver.resolveDestination(queueUrl);
		}
		catch (DestinationResolutionException e) {
			assertThat(e.getMessage()).startsWith(
					"The queue does not exist or no access to perform action sqs:GetQueueUrl.");
		}
	}

	@Test
	public void resolveDestination_withResourceIdResolver_shouldUseIt() throws Exception {
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(anyString()))
				.thenReturn("http://queue.com");
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver
				.resolveDestination("testQueue");

		assertThat(physicalResourceId).isEqualTo("http://queue.com");

	}

	@Test
	public void resolveDestination_withResourceIdResolver_nonUrlId_shouldGetUrlByResolvedName()
			throws Exception {
		String queueUrl = "http://queue.com";
		String resolvedQueueName = "some-queue-name";
		AmazonSQS amazonSqs = mock(AmazonSQS.class);
		when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(resolvedQueueName)))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(anyString()))
				.thenReturn(resolvedQueueName);
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(
				amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver
				.resolveDestination("testQueue");

		assertThat(physicalResourceId).isEqualTo("http://queue.com");
	}

	@Test(expected = IllegalArgumentException.class)
	public void instantiation_withNullAmazonClient_shouldThrowAnError() throws Exception {
		new DynamicQueueUrlDestinationResolver(null, null);
	}

}
