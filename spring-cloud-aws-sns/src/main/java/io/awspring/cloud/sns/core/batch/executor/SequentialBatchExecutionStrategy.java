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
package io.awspring.cloud.sns.core.batch.executor;

import io.awspring.cloud.sns.core.batch.BatchResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.util.Assert;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;
import software.amazon.awssdk.services.sns.model.PublishBatchResultEntry;

/**
 * Default implementation of {@link BatchExecutionStrategy} for executing batch publish operations to AWS SNS.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public class SequentialBatchExecutionStrategy implements BatchExecutionStrategy {

	private final SnsClient snsClient;

	private static final int MAX_SNS_BATCH_SIZE = 10;

	public SequentialBatchExecutionStrategy(SnsClient snsClient) {
		Assert.notNull(snsClient, "SnsClient cannot be null!");
		this.snsClient = snsClient;
	}

	/**
	 * Sends a batch of messages to the specified SNS topic.
	 *
	 * @param topicArn The ARN of the SNS topic to publish to
	 * @param entries Collection of batch request entries to publish
	 * @return BatchResult containing successful results and any errors
	 */
	@Override
	public BatchResult send(Arn topicArn, Collection<PublishBatchRequestEntry> entries) {
		Assert.notNull(topicArn, "topicArn is required");
		Assert.notNull(topicArn, "entries are required");

		List<BatchResult.SnsResult> allResults = new ArrayList<>();
		List<BatchResult.SnsError> allErrors = new ArrayList<>();
		List<PublishBatchRequestEntry> batch = new ArrayList<>(MAX_SNS_BATCH_SIZE);

		for (PublishBatchRequestEntry entry : entries) {
			batch.add(entry);

			if (batch.size() == MAX_SNS_BATCH_SIZE) {
				processBatch(topicArn, batch, allResults, allErrors);
				batch.clear();
			}
		}

		if (!batch.isEmpty()) {
			processBatch(topicArn, batch, allResults, allErrors);
		}

		return new BatchResult(allResults, allErrors);
	}

	private void processBatch(Arn topicArn, List<PublishBatchRequestEntry> batch, List<BatchResult.SnsResult> results,
			List<BatchResult.SnsError> errors) {
		PublishBatchRequest publishBatchRequest = PublishBatchRequest.builder().topicArn(topicArn.toString())
				.publishBatchRequestEntries(batch).build();

		PublishBatchResponse response = snsClient.publishBatch(publishBatchRequest);

		for (PublishBatchResultEntry res : response.successful()) {
			results.add(new BatchResult.SnsResult(res.messageId(), res.id(), res.sequenceNumber()));
		}

		for (BatchResultErrorEntry error : response.failed()) {
			errors.add(new BatchResult.SnsError(error.id(), error.code(), error.message(), error.senderFault()));
		}
	}
}
