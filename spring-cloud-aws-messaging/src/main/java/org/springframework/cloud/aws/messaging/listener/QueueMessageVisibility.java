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

package org.springframework.cloud.aws.messaging.listener;

import java.util.concurrent.Future;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;

/**
 * @author Szymon Dembek
 * @since 1.3
 */
public class QueueMessageVisibility implements Visibility {

	private final AmazonSQSAsync amazonSqsAsync;

	private final String queueUrl;

	private final String receiptHandle;

	public QueueMessageVisibility(AmazonSQSAsync amazonSqsAsync, String queueUrl,
			String receiptHandle) {
		this.amazonSqsAsync = amazonSqsAsync;
		this.queueUrl = queueUrl;
		this.receiptHandle = receiptHandle;
	}

	@Override
	public Future<?> extend(int seconds) {
		return this.amazonSqsAsync
				.changeMessageVisibilityAsync(new ChangeMessageVisibilityRequest()
						.withQueueUrl(this.queueUrl).withReceiptHandle(this.receiptHandle)
						.withVisibilityTimeout(seconds));
	}

}
