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
package io.awspring.cloud.sns.integration;

import io.awspring.cloud.sns.core.CachingTopicArnResolver;
import io.awspring.cloud.sns.core.SnsAsyncTopicArnResolver;
import io.awspring.cloud.sns.core.SnsHeaders;
import io.awspring.cloud.sns.core.TopicArnResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * The {@link AbstractMessageProducingHandler} implementation to send SNS Notifications
 * ({@link SnsAsyncClient#publish(PublishRequest)}) to the provided {@code topicArn} (or evaluated at runtime against
 * {@link Message}).
 * <p>
 * The SNS Message subject can be evaluated as a result of {@link #subjectExpression}.
 * <p>
 * The algorithm to populate an SNS Message body is like:
 * <ul>
 * <li>If the {@code payload instanceof PublishRequest} it is used as is for publishing.</li>
 * <li>If the {@link #bodyExpression} is specified, it is used to be evaluated against {@code requestMessage}.</li>
 * <li>If the evaluation result (or {@code payload}) is instance of {@link SnsBodyBuilder}, the SNS Message is built
 * from there and the {@code messageStructure} of the {@link PublishRequest} is set to {@code json}. For the convenience
 * the package {@code org.springframework.integration.aws.support} is imported to the {@link #evaluationContext} to
 * allow bypassing it for the {@link SnsBodyBuilder} from the {@link #bodyExpression} definition. For example:
 *
 * <pre class="code">
 * {@code
 * String bodyExpression = "SnsBodyBuilder.withDefault(payload).forProtocols(payload.substring(0, 140), 'sms')";
 * snsMessageHandler.setBodyExpression(spelExpressionParser.parseExpression(bodyExpression));
 * }
 * </pre>
 *
 * </li>
 * <li>Otherwise the {@code payload} (or the {@link #bodyExpression} evaluation result) is converted to the
 * {@link String} using {@link #getConversionService()}.</li>
 * </ul>
 *
 * @author Artem Bilan
 * @author Christopher Smith
 *
 * @since 4.0
 *
 * @see SnsAsyncClient
 * @see PublishRequest
 * @see SnsBodyBuilder
 */
public class SnsMessageHandler extends AbstractMessageProducingHandler {

	private final SnsAsyncClient amazonSns;

	private Expression topicArnExpression;

	private TopicArnResolver topicArnResolver;

	private Expression subjectExpression;

	private Expression messageGroupIdExpression;

	private Expression messageDeduplicationIdExpression;

	private Expression bodyExpression;

	protected static final long DEFAULT_SEND_TIMEOUT = 10000;

	private EvaluationContext evaluationContext;

	private Expression sendTimeoutExpression = new ValueExpression<>(DEFAULT_SEND_TIMEOUT);

	private HeaderMapper<Map<String, MessageAttributeValue>> headerMapper;

	private boolean headerMapperSet;

	public SnsMessageHandler(SnsAsyncClient amazonSns) {
		Assert.notNull(amazonSns, "amazonSns must not be null.");
		this.amazonSns = amazonSns;
		this.topicArnResolver = new CachingTopicArnResolver(new SnsAsyncTopicArnResolver(this.amazonSns));
	}

	public void setTopicArn(String topicArn) {
		Assert.hasText(topicArn, "topicArn must not be empty.");
		this.topicArnExpression = new LiteralExpression(topicArn);
	}

	public void setTopicArnExpression(Expression topicArnExpression) {
		Assert.notNull(topicArnExpression, "topicArnExpression must not be null.");
		this.topicArnExpression = topicArnExpression;
	}

	/**
	 * Provide a custom {@link TopicArnResolver}; defaults to {@link SnsAsyncTopicArnResolver}.
	 * @param topicArnResolver the {@link TopicArnResolver} to use.
	 */
	public void setTopicArnResolver(TopicArnResolver topicArnResolver) {
		Assert.notNull(topicArnResolver, "'topicArnResolver' must not be null.");
		this.topicArnResolver = topicArnResolver;
	}

	public void setSubject(String subject) {
		Assert.hasText(subject, "subject must not be empty.");
		this.subjectExpression = new LiteralExpression(subject);
	}

	public void setSubjectExpression(Expression subjectExpression) {
		Assert.notNull(subjectExpression, "subjectExpression must not be null.");
		this.subjectExpression = subjectExpression;
	}

	/**
	 * A fixed message-group ID to be set for messages sent to an SNS FIFO topic from this handler. Equivalent to
	 * calling {{@link #setMessageGroupIdExpression(Expression)} with a literal string expression.
	 * @param messageGroupId the group ID to be used for all messages sent from this handler
	 */
	public void setMessageGroupId(String messageGroupId) {
		Assert.hasText(messageGroupId, "messageGroupId must not be empty.");
		this.messageGroupIdExpression = new LiteralExpression(messageGroupId);
	}

	/**
	 * The {@link Expression} to determine the
	 * <a href="https://docs.aws.amazon.com/sns/latest/dg/fifo-message-grouping.html">message group</a> for messages
	 * sent to an SNS FIFO topic from this handler.
	 * @param messageGroupIdExpression the {@link Expression} to produce the message-group ID
	 */
	public void setMessageGroupIdExpression(Expression messageGroupIdExpression) {
		Assert.notNull(messageGroupIdExpression, "messageGroupIdExpression must not be null.");
		this.messageGroupIdExpression = messageGroupIdExpression;
	}

	/**
	 * The {@link Expression} to determine the deduplication ID for this message. SNS FIFO topics
	 * <a href="https://docs.aws.amazon.com/sns/latest/dg/fifo-message-dedup.html">require a message deduplication ID to
	 * be specified</a>, either in the adapter configuration or on a {@link PublishRequest} payload of the request
	 * {@link Message}, unless content-based deduplication is enabled on the topic.
	 * @param messageDeduplicationIdExpression the {@link Expression} to produce the message deduplication ID
	 */
	public void setMessageDeduplicationIdExpression(Expression messageDeduplicationIdExpression) {
		Assert.notNull(messageDeduplicationIdExpression, "messageDeduplicationIdExpression must not be null.");
		this.messageDeduplicationIdExpression = messageDeduplicationIdExpression;
	}

	/**
	 * The {@link Expression} to produce the SNS notification message. If it evaluates to the {@link SnsBodyBuilder} the
	 * {@code messageStructure} of the {@link PublishRequest} is set to {@code json}. Otherwise, the
	 * {@link #getConversionService()} is used to convert the evaluation result to the {@link String} without setting
	 * the {@code messageStructure}.
	 * @param bodyExpression the {@link Expression} to produce the SNS notification message.
	 */
	public void setBodyExpression(Expression bodyExpression) {
		Assert.notNull(bodyExpression, "bodyExpression must not be null.");
		this.bodyExpression = bodyExpression;
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

	/**
	 * Specify a {@link HeaderMapper} to map outbound headers.
	 * @param headerMapper the {@link HeaderMapper} to map outbound headers.
	 */
	public void setHeaderMapper(HeaderMapper<Map<String, MessageAttributeValue>> headerMapper) {
		this.headerMapper = headerMapper;
		this.headerMapperSet = true;
	}

	@Override
	public String getComponentType() {
		return "aws:sns-outbound-channel-adapter";
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (!this.headerMapperSet) {
			setHeaderMapper(new SnsHeaderMapper());
		}
		TypeLocator typeLocator = this.evaluationContext.getTypeLocator();
		if (typeLocator instanceof StandardTypeLocator standardTypeLocator) {
			/*
			 * Register the 'io.awspring.cloud.sns.integration' package you don't need a FQCN for the 'SnsBodyBuilder'.
			 */
			standardTypeLocator.registerImport("io.awspring.cloud.sns.integration");
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		PublishRequest request = messageToAwsRequest(message);
		CompletableFuture<?> resultFuture = this.amazonSns.publish(request)
				.handle((response, ex) -> handleResponse(message, request, response, ex));

		if (isAsync()) {
			sendOutputs(resultFuture, message);
			return;
		}

		Long sendTimeout = this.sendTimeoutExpression.getValue(this.evaluationContext, message, Long.class);
		try {
			if (sendTimeout == null || sendTimeout < 0) {
				resultFuture.get();
			}
			else {
				resultFuture.get(sendTimeout, TimeUnit.MILLISECONDS);
			}
		}
		catch (TimeoutException te) {
			throw new MessageTimeoutException(message, "Timeout waiting for response from AmazonKinesis", te);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
		catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected Message<?> handleResponse(Message<?> message, PublishRequest request, PublishResponse response,
			Throwable cause) {

		if (cause != null) {
			throw new SnsRequestFailureException(message, request, cause);
		}
		return getMessageBuilderFactory().fromMessage(message)
				.copyHeadersIfAbsent(additionalOnSuccessHeaders(request, response)).build();
	}

	private PublishRequest messageToAwsRequest(Message<?> message) {
		Object payload = message.getPayload();

		if (payload instanceof PublishRequest publishRequest) {
			return publishRequest;
		}
		else {
			Assert.state(this.topicArnExpression != null, "'topicArn' or 'topicArnExpression' must be specified.");
			PublishRequest.Builder publishRequest = PublishRequest.builder();
			String topic = this.topicArnExpression.getValue(this.evaluationContext, message, String.class);
			String topicArn = this.topicArnResolver.resolveTopicArn(topic).toString();
			publishRequest.topicArn(topicArn);

			if (this.subjectExpression != null) {
				String subject = this.subjectExpression.getValue(this.evaluationContext, message, String.class);
				publishRequest.subject(subject);
			}

			if (topicArn.endsWith(".fifo")) {
				String messageGroupId = null;
				if (this.messageGroupIdExpression != null) {
					messageGroupId = this.messageGroupIdExpression.getValue(this.evaluationContext, message,
							String.class);
				}
				Assert.notNull(messageGroupId, () -> "The 'messageGroupIdExpression' [" + this.messageGroupIdExpression
						+ "] " + "must not evaluate to null. The failed request message is " + message);

				publishRequest.messageGroupId(messageGroupId);

				String messageDeduplicationId = null;
				if (this.messageDeduplicationIdExpression != null) {
					messageDeduplicationId = this.messageDeduplicationIdExpression.getValue(this.evaluationContext,
							message, String.class);
				}
				Assert.notNull(messageDeduplicationId,
						() -> "The 'messageDeduplicationIdExpression' [" + this.messageDeduplicationIdExpression + "] "
								+ "must not evaluate to null. The failed request message is " + message);

				publishRequest.messageDeduplicationId(messageDeduplicationId);
			}
			else if (this.messageGroupIdExpression != null || this.messageDeduplicationIdExpression != null) {
				logger.info("The 'messageGroupIdExpression' and 'messageDeduplicationIdExpression' properties "
						+ "are ignored for non-FIFO topics.");
			}

			Object snsMessage = message.getPayload();

			if (this.bodyExpression != null) {
				snsMessage = this.bodyExpression.getValue(this.evaluationContext, message);
			}

			if (snsMessage instanceof SnsBodyBuilder) {
				publishRequest.messageStructure("json").message(((SnsBodyBuilder) snsMessage).build());
			}
			else {
				publishRequest.message(getConversionService().convert(snsMessage, String.class));
			}

			if (this.headerMapper != null) {
				mapHeaders(message, publishRequest, this.headerMapper);
			}
			return publishRequest.build();
		}
	}

	private void mapHeaders(Message<?> message, PublishRequest.Builder publishRequest,
			HeaderMapper<Map<String, MessageAttributeValue>> headerMapper) {

		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		headerMapper.fromHeaders(message.getHeaders(), messageAttributes);
		if (!messageAttributes.isEmpty()) {
			publishRequest.messageAttributes(messageAttributes);
		}
	}

	protected Map<String, ?> additionalOnSuccessHeaders(PublishRequest request, PublishResponse response) {
		return Map.of(SnsHeaders.TOPIC_HEADER, request.topicArn(), SnsHeaders.MESSAGE_ID_HEADER, response.messageId());
	}

}
