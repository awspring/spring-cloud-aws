package io.awspring.cloud.sqs.operations;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public interface SqsSendOptions<T, O extends SqsSendOptions<T, O>> {

	O queue(String queue);

	O payload(T payload);

	O header(String headerName, Object headerValue);

	O headers(Map<String, Object> headers);

	O delay(Duration delay);

	interface Standard<T> extends SqsSendOptions<T, Standard<T>> {
	}

	interface Fifo<T> extends SqsSendOptions<T, Fifo<T>> {

		Fifo<T> messageGroupId(UUID messageGroupId);

		Fifo<T> messageDeduplicationId(UUID messageDeduplicationId);

	}

}
