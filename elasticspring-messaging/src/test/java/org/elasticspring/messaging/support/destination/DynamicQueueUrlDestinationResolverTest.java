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
import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DynamicQueueUrlDestinationResolverTest {

	@Test
	public void testAutoCreate() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		String queueUrl = "http://foo/bar";
		Mockito.when(amazonSqs.createQueue(new CreateQueueRequest("foo"))).thenReturn(new CreateQueueResult().withQueueUrl(queueUrl));

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(amazonSqs);
		dynamicQueueDestinationResolver.setAutoCreate(true);
		Assert.assertEquals(queueUrl, dynamicQueueDestinationResolver.resolveDestination("foo"));
	}

	@Test
	public void testAbsoluteUrl() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(amazonSqs);
		String destination = "http://sqs-amazon.aws.com/123123123/myQueue";
		Assert.assertEquals(destination, dynamicQueueDestinationResolver.resolveDestination(destination));
	}

	@Test
	public void testNoAutoCreate() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		String queueUrl = "http://foo/bar";
		Mockito.when(amazonSqs.getQueueUrl(new GetQueueUrlRequest("foo"))).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));

		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(amazonSqs);
		Assert.assertEquals(queueUrl, dynamicQueueDestinationResolver.resolveDestination("foo"));
	}

	@Test
	public void testInvalidDestinationName() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		AmazonServiceException exception = new AmazonServiceException("AWS.SimpleQueueService.NonExistentQueue");
		exception.setErrorCode("AWS.SimpleQueueService.NonExistentQueue");
		String queueUrl = "invalidName";
		Mockito.when(amazonSqs.getQueueUrl(new GetQueueUrlRequest(queueUrl))).thenThrow(exception);
		DynamicQueueUrlDestinationResolver dynamicQueueDestinationResolver = new DynamicQueueUrlDestinationResolver(amazonSqs);
		try {
			dynamicQueueDestinationResolver.resolveDestination(queueUrl);
		} catch (InvalidDestinationException e) {
			Assert.assertEquals(queueUrl, e.getDestinationName());
		}
	}

	@Test
	public void resolveDestination_withResourceIdResolver_shouldUseIt() throws Exception {
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);
		ResourceIdResolver resourceIdResolver = Mockito.mock(ResourceIdResolver.class);
		Mockito.when(resourceIdResolver.resolveToPhysicalResourceId(Mockito.anyString())).thenReturn("http://queue.com");
		DynamicQueueUrlDestinationResolver dynamicQueueUrlDestinationResolver = new DynamicQueueUrlDestinationResolver(amazonSqs, resourceIdResolver);

		String physicalResourceId = dynamicQueueUrlDestinationResolver.resolveDestination("testQueue");

		Assert.assertEquals("http://queue.com", physicalResourceId);

	}
}
