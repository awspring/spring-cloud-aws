package io.awspring.cloud.sqs;

import io.awspring.cloud.sqs.listener.QueueAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class QueueAttributesProvider {

	private static final Logger logger = LoggerFactory.getLogger(QueueAttributes.class);

	private QueueAttributesProvider() {
	}

	public static QueueAttributes fetch(String queueName, SqsAsyncClient sqsAsyncClient, Collection<QueueAttributeName> queueAttributeNames) {
		try {
			String queueUrl = resolveQueueUrl(queueName, sqsAsyncClient);
			return new QueueAttributes(queueName, queueUrl, getQueueAttributes(sqsAsyncClient, queueAttributeNames, queueUrl, queueName));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while fetching attributes for queue " + queueName, e);
		}
		catch (ExecutionException e) {
			throw new IllegalStateException("ExecutionException while fetching attributes for queue " + queueName, e);
		}
	}

	private static String resolveQueueUrl(String queueName, SqsAsyncClient sqsAsyncClient) throws InterruptedException, ExecutionException {
		return isValidQueueUrl(queueName)
			? queueName
			: sqsAsyncClient.getQueueUrl(req -> req.queueName(queueName)).get().queueUrl();
	}

	private static Map<QueueAttributeName, String> getQueueAttributes(SqsAsyncClient sqsAsyncClient, Collection<QueueAttributeName> queueAttributeNames, String queueUrl, String queueName) throws InterruptedException, ExecutionException {
		return queueAttributeNames.isEmpty()
			? Collections.emptyMap()
			: doGetAttributes(sqsAsyncClient, queueAttributeNames, queueUrl, queueName);
	}

	private static Map<QueueAttributeName, String> doGetAttributes(SqsAsyncClient sqsAsyncClient, Collection<QueueAttributeName> queueAttributeNames, String queueUrl, String queueName) throws InterruptedException, ExecutionException {
		logger.debug("Fetching attributes {} for queue {}", queueAttributeNames, queueName);
		Map<QueueAttributeName, String> attributes = sqsAsyncClient.getQueueAttributes(req -> req.queueUrl(queueUrl).attributeNames(queueAttributeNames)).get().attributes();
		logger.debug("Attributes for queue {} received", queueName);
		return attributes;
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


}
