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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.CompletableFutures;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/**
 * Resolves {@link QueueAttributes} for the specified queue. Fetchs the queue url for a queue name, unless a url is
 * specified as name. If such queue is not found, either creates the queue or fails according to the specified
 * {@link QueueNotFoundStrategy}. After the url is resolved, retrieves the queue attributes specified in the
 * {@link QueueAttributeName} collection.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see ContainerOptions#getQueueAttributeNames()
 * @see ContainerOptions#getQueueNotFoundStrategy()
 */
public class QueueAttributesResolver {

	private static final Logger logger = LoggerFactory.getLogger(QueueAttributesResolver.class);

	private final String queueName;

	private final SqsAsyncClient sqsAsyncClient;

	private final Collection<QueueAttributeName> queueAttributeNames;

	private final QueueNotFoundStrategy queueNotFoundStrategy;

	private QueueAttributesResolver(Builder builder) {
		this.queueName = builder.queueName;
		this.sqsAsyncClient = builder.sqsAsyncClient;
		this.queueAttributeNames = builder.queueAttributeNames;
		this.queueNotFoundStrategy = builder.queueNotFoundStrategy;
	}

	public static Builder builder() {
		return new Builder();
	}

	// @formatter:off
	public CompletableFuture<QueueAttributes> resolveQueueAttributes() {
		logger.debug("Resolving attributes for queue {}", this.queueName);
		return resolveQueueUrl()
			.thenCompose(queueUrl -> getQueueAttributes(queueUrl)
				.thenApply(queueAttributes -> new QueueAttributes(this.queueName, queueUrl, queueAttributes)));
	}

	private CompletableFuture<String> resolveQueueUrl() {
		return isValidQueueUrl(this.queueName)
			? CompletableFuture.completedFuture(this.queueName)
			: doResolveQueueUrl();
	}

	private CompletableFuture<String> doResolveQueueUrl() {
		return CompletableFutures
			.exceptionallyCompose(this.sqsAsyncClient.getQueueUrl(req -> req.queueName(this.queueName))
				.thenApply(GetQueueUrlResponse::queueUrl), this::handleException);
	}

	private CompletableFuture<String> handleException(Throwable t) {
		return t.getCause() instanceof QueueDoesNotExistException
			&& QueueNotFoundStrategy.CREATE.equals(this.queueNotFoundStrategy)
				? createQueue()
				: CompletableFutures.failedFuture(t);
	}

	private CompletableFuture<String> createQueue() {
		return this.sqsAsyncClient.createQueue(req -> req.queueName(this.queueName).build())
			.thenApply(CreateQueueResponse::queueUrl)
			.whenComplete(this::logCreateQueueResult);
	}

	private void logCreateQueueResult(String v, Throwable t) {
		if (t != null) {
			logger.debug("Error creating queue {}", this.queueName, t);
			return;
		}
		logger.debug("Created queue {} with url {}", this.queueName, v);
	}

	private CompletableFuture<Map<QueueAttributeName, String>> getQueueAttributes(String queueUrl) {
		return this.queueAttributeNames.isEmpty()
			? CompletableFuture.completedFuture(Collections.emptyMap())
			: doGetAttributes(queueUrl);
	}

	private CompletableFuture<Map<QueueAttributeName, String>> doGetAttributes(String queueUrl) {
		logger.debug("Resolving attributes {} for queue {}", this.queueAttributeNames, this.queueName);
		return this.sqsAsyncClient
			.getQueueAttributes(req -> req.queueUrl(queueUrl).attributeNames(this.queueAttributeNames))
			.thenApply(GetQueueAttributesResponse::attributes)
			.whenComplete((v, t) -> logger.debug("Attributes for queue {} resolved", this.queueName));
	}
	// @formatter:on

	private boolean isValidQueueUrl(String name) {
		try {
			URI candidate = new URI(name);
			return ("http".equals(candidate.getScheme()) || "https".equals(candidate.getScheme()));
		}
		catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * A builder for creating {@link QueueAttributesResolver} instances.
	 */
	public static class Builder {

		private String queueName;

		private SqsAsyncClient sqsAsyncClient;

		private Collection<QueueAttributeName> queueAttributeNames;

		private QueueNotFoundStrategy queueNotFoundStrategy;

		/**
		 * The queue name. The queue url can also be specified.
		 * @param queueName the queue name.
		 * @return the builder instance.
		 */
		public Builder queueName(String queueName) {
			Assert.notNull(queueName, "queueName cannot be null");
			this.queueName = queueName;
			return this;
		}

		/**
		 * The {@link SqsAsyncClient} to be used to resolve the queue attributes.
		 * @param sqsAsyncClient the client instance.
		 * @return the builder instance.
		 */
		public Builder sqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
			Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
			this.sqsAsyncClient = sqsAsyncClient;
			return this;
		}

		/**
		 * The {@link QueueAttributeName}s to be retrieved.
		 * @param queueAttributeNames the attributes names.
		 * @return the builder instance.
		 */
		public Builder queueAttributeNames(Collection<QueueAttributeName> queueAttributeNames) {
			Assert.notNull(queueAttributeNames, "queueAttributeNames cannot be null");
			this.queueAttributeNames = queueAttributeNames;
			return this;
		}

		/**
		 * The strategy to be used in case a queue does not exist.
		 * @param queueNotFoundStrategy the strategy.
		 * @return the builder instance.
		 */
		public Builder queueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy) {
			Assert.notNull(queueNotFoundStrategy, "queueNotFoundStrategy cannot be null");
			this.queueNotFoundStrategy = queueNotFoundStrategy;
			return this;
		}

		/**
		 * Build the {@link QueueAttributesResolver} instance with the provided settings.
		 * @return the created instance.
		 */
		public QueueAttributesResolver build() {
			return new QueueAttributesResolver(this);
		}
	}

}
