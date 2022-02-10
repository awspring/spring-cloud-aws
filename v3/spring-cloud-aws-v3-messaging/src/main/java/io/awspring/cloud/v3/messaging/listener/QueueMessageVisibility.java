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

package io.awspring.cloud.v3.messaging.listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;

/**
 * @author Szymon Dembek
 * @since 1.3
 */
public class QueueMessageVisibility implements Visibility {

	private final SqsClient amazonSqsAsync;

	private final String queueUrl;

	private final String receiptHandle;

	public QueueMessageVisibility(SqsClient amazonSqsAsync, String queueUrl, String receiptHandle) {
		this.amazonSqsAsync = amazonSqsAsync;
		this.queueUrl = queueUrl;
		this.receiptHandle = receiptHandle;
	}

	@Override
	public Future<?> extend(int seconds) {
		return CompletableFuture.supplyAsync(() ->
				this.amazonSqsAsync.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
						.queueUrl(this.queueUrl)
						.receiptHandle(this.receiptHandle)
						.visibilityTimeout(seconds)
					.build()));
	}

}
