/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsAsyncOperations;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMessageProducingHandler} implementation for the Amazon SQS. All the logic based on the
 * {@link SqsAsyncOperations#sendAsync(String, Message)} or {@link SqsAsyncOperations#sendManyAsync(String, Collection)}
 * if the request message's payload is a collection of {@link Message} instances.
 * <p>
 * All the SQS-specific message attributes have to be provided in the respective message headers via
 * {@link SqsHeaders.MessageSystemAttributes} constant values or with the {@link SqsAsyncOperations}.
 * <p>
 * This {@link AbstractMessageProducingHandler} produces a reply only in the {@link #isAsync()} mode. For a single
 * request message the {@link SendResult} is converted to the reply message with respective headers. The
 * {@link SendResult.Batch} is sent as a reply message's payload as is.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 *
 * @see SqsAsyncOperations#sendAsync
 * @see SqsAsyncOperations#sendManyAsync
 * @see SqsHeaders.MessageSystemAttributes
 */
public class SqsMessageHandler extends AbstractMessageProducingHandler {

	public static final long DEFAULT_SEND_TIMEOUT = 10000;

	private final SqsAsyncOperations sqsAsyncOperations;

	private Expression queueExpression;

	private EvaluationContext evaluationContext;

	private Expression sendTimeoutExpression = new ValueExpression<>(DEFAULT_SEND_TIMEOUT);

	public SqsMessageHandler(SqsAsyncOperations sqsAsyncOperations) {
		this.sqsAsyncOperations = sqsAsyncOperations;
	}

	public void setQueue(String queue) {
		setQueueExpression(new LiteralExpression(queue));
	}

	public void setQueueExpressionString(String queueExpression) {
		setQueueExpression(EXPRESSION_PARSER.parseExpression(queueExpression));
	}

	public void setQueueExpression(Expression queueExpression) {
		this.queueExpression = queueExpression;
	}

	public void setSendTimeout(long sendTimeout) {
		setSendTimeoutExpression(new ValueExpression<>(sendTimeout));
	}

	public void setSendTimeoutExpressionString(String sendTimeoutExpression) {
		setSendTimeoutExpression(EXPRESSION_PARSER.parseExpression(sendTimeoutExpression));
	}

	public void setSendTimeoutExpression(Expression sendTimeoutExpression) {
		Assert.notNull(sendTimeoutExpression, "'sendTimeoutExpression' must not be null");
		this.sendTimeoutExpression = sendTimeoutExpression;
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.queueExpression, "The SQS queue must be provided.");
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleMessageInternal(Message<?> message) {
		String queueName = this.queueExpression.getValue(this.evaluationContext, message, String.class);
		Assert.hasText(queueName, "The 'queueExpression' must not evaluate to empty String.");
		CompletableFuture<?> resultFuture;
		if (message.getPayload() instanceof Collection<?> collection) {
			Assert.notEmpty(collection, "The payload with a collection of messages must not be empty.");
			Object next = collection.iterator().next();
			Assert.isInstanceOf(Message.class, next,
					"The payload with a collection of messages must contain 'Message' instances only.");
			Collection<Message<Object>> messages = (Collection<Message<Object>>) collection;

			resultFuture = this.sqsAsyncOperations.sendManyAsync(queueName, messages)
					.thenApply((batchResult) -> getMessageBuilderFactory().withPayload(batchResult).build());
		}
		else {
			resultFuture = this.sqsAsyncOperations.sendAsync(queueName, message)
					.thenApply((sendResult) -> getMessageBuilderFactory().fromMessage(sendResult.message())
							.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, sendResult.endpoint())
							.setHeader(SqsHeaders.MessageSystemAttributes.MESSAGE_ID, sendResult.messageId())
							.copyHeaders(sendResult.additionalInformation()).build());
		}

		if (isAsync()) {
			sendOutputs(resultFuture, message);
			return;
		}

		Long sendTimeout = this.sendTimeoutExpression.getValue(this.evaluationContext, message, Long.class);
		if (sendTimeout == null || sendTimeout < 0) {
			try {
				resultFuture.get();
			}
			catch (InterruptedException | ExecutionException ex) {
				throw new IllegalStateException(ex);
			}
		}
		else {
			try {
				resultFuture.get(sendTimeout, TimeUnit.MILLISECONDS);
			}
			catch (TimeoutException te) {
				throw new MessageTimeoutException(message, "Timeout waiting for response from Amazon SQS", te);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ex);
			}
			catch (ExecutionException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
