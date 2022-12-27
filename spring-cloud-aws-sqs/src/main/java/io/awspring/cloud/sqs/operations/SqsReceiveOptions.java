package io.awspring.cloud.sqs.operations;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public interface SqsReceiveOptions<T, O extends SqsReceiveOptions<T, O>> {

	O queue(String queue);

	O pollTimeout(Duration pollTimeout);

	O payloadClass(Class<T> payloadClass);

	O visibilityTimeout(Duration visibilityTimeout);

	O additionalHeader(String name, Object value);

	O additionalHeaders(Map<String, Object> headers);

	O maxNumberOfMessages(Integer maxNumberOfMessages);

	interface Standard<T> extends SqsReceiveOptions<T, Standard<T>> {
	}

	interface Fifo<T> extends SqsReceiveOptions<T, Fifo<T>> {

		Fifo<T> receiveRequestAttemptId(UUID receiveRequestAttemptId);

	}

}
