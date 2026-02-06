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
import java.util.Collection;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;

/**
 * Strategy interface for executing batch publish operations to SNS.
 * <p>
 * Implementations of this interface handle the actual communication with SNS to publish a batch of messages to a topic.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public interface BatchExecutionStrategy {

	/**
	 * Sends a batch of messages to the specified SNS topic.
	 * <p>
	 * Executes the batch publish operation and returns results containing both successful message IDs and any errors
	 * that occurred.
	 *
	 * @param topicArn The ARN of the SNS topic to publish to
	 * @param entries Collection of batch request entries to publish
	 * @return BatchResult containing successful results and any errors
	 */
	BatchResult send(Arn topicArn, Collection<PublishBatchRequestEntry> entries);
}
